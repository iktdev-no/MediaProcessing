package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping

import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams.AudioArguments
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams.VideoArguments
import no.iktdev.mediaprocessing.shared.common.contract.data.EncodeArgumentData
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.EncodingPreference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoStream
import java.io.File

class EncodeWorkArgumentsMapping(
    val inputFile: String,
    val outFileFullName: String,
    val outFileAbsolutePathFile: File,
    val streams: ParsedMediaStreams,
    val preference: EncodingPreference
) {

    fun getArguments(): EncodeArgumentData? {
        val outVideoFileAbsolutePath = outFileAbsolutePathFile.using("${outFileFullName}.mp4").absolutePath
        val vaas = VideoAndAudioSelector(streams, preference)
        val vArg = vaas.getVideoStream()
            ?.let { VideoArguments(it, streams, preference.video).getVideoArguments() }
        val aArg = vaas.getAudioStream()
            ?.let { AudioArguments(it, streams, preference.audio).getAudioArguments() }

        val vaArgs = toFfmpegWorkerArguments(vArg, aArg)
        return if (vaArgs.isEmpty()) {
            null
        } else {
            EncodeArgumentData(
                inputFile = inputFile,
                outputFile = outVideoFileAbsolutePath,
                arguments = vaArgs
            )
        }
    }


    private class VideoAndAudioSelector(val mediaStreams: ParsedMediaStreams, val preference: EncodingPreference) {
        private var defaultVideoSelected: VideoStream? = mediaStreams.videoStream
            .filter { (it.duration_ts ?: 0) > 0 }
            .maxByOrNull { it.duration_ts ?: 0 } ?: mediaStreams.videoStream.minByOrNull { it.index }
        private var defaultAudioSelected: AudioStream? = mediaStreams.audioStream
            .filter { (it.duration_ts ?: 0) > 0 }
            .maxByOrNull { it.duration_ts ?: 0 } ?: mediaStreams.audioStream.minByOrNull { it.index }

        fun getVideoStream(): VideoStream? {
            return defaultVideoSelected
        }

        fun getAudioStream(): AudioStream? {
            val languageFiltered = mediaStreams.audioStream.filter { it.tags.language == preference.audio.language }
            val channeledAndCodec = languageFiltered.find {
                it.channels >= (preference.audio.channels ?: 2) && it.codec_name == preference.audio.codec.lowercase()
            }
            return channeledAndCodec ?: return languageFiltered.minByOrNull { it.index } ?: defaultAudioSelected
        }

    }
}