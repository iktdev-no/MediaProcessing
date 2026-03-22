package no.iktdev.mediaprocessing.processer.runners

import com.google.gson.Gson
import no.iktdev.mediaprocessing.ffmpeg.FFprobe
import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.FFprobeFormat
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.files.IFile

class ProbeRunner(
    val file: IFile,
    val executable: String
) : Runner() {

    override suspend fun run(): RunnerResult<ProbePayload> {
        val gson = Gson()

        val result = getFfprobe().readJsonStreams(file.absolutePath)
        val jStreams = result.data?.getAsJsonArray("streams")
            ?: return RunnerResult.Reject("Missing streams in ffprobe output")

        val jFormat = result.data?.getAsJsonObject("format")
            ?: return RunnerResult.Reject("Missing format in ffprobe output")

        val videoStreams = mutableListOf<VideoStream>()
        val audioStreams = mutableListOf<AudioStream>()

        for (streamJson in jStreams) {
            val streamObject = streamJson.asJsonObject
            if (!streamObject.has("codec_name")) continue
            val codecType = streamObject.get("codec_type").asString

            when (codecType) {
                "video" -> videoStreams.add(gson.fromJson(streamObject, VideoStream::class.java))
                "audio" -> audioStreams.add(gson.fromJson(streamObject, AudioStream::class.java))
            }
        }

        val parsedFormat = gson.fromJson(jFormat, FFprobeFormat::class.java)

        return RunnerResult.Success(
            ProbePayload(
                format = parsedFormat,
                videoStreams = videoStreams,
                audioStreams = audioStreams
            )
        )
    }

    private fun getFfprobe(): FFprobe =
        JsonFfinfo(executable)

    class JsonFfinfo(executable: String) : FFprobe(executable)


    data class ProbePayload(
        val format: FFprobeFormat,
        val videoStreams: List<VideoStream>,
        val audioStreams: List<AudioStream>
    )

}