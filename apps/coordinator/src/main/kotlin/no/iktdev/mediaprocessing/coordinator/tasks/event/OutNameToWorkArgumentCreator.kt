package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.Preference
import no.iktdev.mediaprocessing.shared.SharedConfig
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.*
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.mediaprocessing.shared.persistance.PersistentMessage
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

/**
 * Is to be called or to run with the result from FileOout
 */
@Service
class OutNameToWorkArgumentCreator(@Autowired coordinator: Coordinator) : TaskCreator() {
    private val log = KotlinLogging.logger {}

    init {
        coordinator.addListener(this)
    }

    override fun isPrerequisitesOk(events: List<PersistentMessage>): Boolean {
        val required = listOf(
            KafkaEvents.EVENT_PROCESS_STARTED.event,
            KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED.event,
            KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED.event
        )
        return events.filter { it.eventId in required }.all { it.data.isSuccess() }
    }

    override fun onEventReceived(referenceId: String, event: PersistentMessage, events: List<PersistentMessage>) {
        val preference = Preference.getPreference()
        if (event.event != KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED)
            return

        if (!isPrerequisitesOk(events)) {
            return
        }
        val inputFile = events.find { it.data is ProcessStarted }?.data as ProcessStarted
        val baseInfo = events.findLast { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed
        val readStreamsEvent = events.find { it.data is MediaStreamsParsePerformed }?.data as MediaStreamsParsePerformed
        val serializedParsedStreams =
            Gson().fromJson<ParsedMediaStreams>(readStreamsEvent.parsedAsJson, ParsedMediaStreams::class.java)

        val outDir = SharedConfig.outgoingContent.using(baseInfo.title)

        getFfmpegVideoArguments(
            inputFile = inputFile.file,
            outDir = outDir,
            preference = preference.encodePreference,
            baseInfo = baseInfo,
            serializedParsedStreams = serializedParsedStreams
        ).let { producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED, it) }

        getFfmpegSubtitleArguments(
            inputFile = inputFile.file,
            outDir = outDir,
            baseInfo = baseInfo,
            serializedParsedStreams = serializedParsedStreams
        ).let { producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED, it) }


    }

    private fun getFfmpegVideoArguments(
        inputFile: String,
        outDir: File,
        preference: EncodingPreference,
        baseInfo: BaseInfoPerformed,
        serializedParsedStreams: ParsedMediaStreams
    ): MessageDataWrapper {
        val outVideoFile = outDir.using("${baseInfo.sanitizedName}.mp4").absolutePath

        val vaas = VideoAndAudioSelector(serializedParsedStreams, preference)

        val vArg = vaas.getVideoStream()?.let { VideoArguments(it, serializedParsedStreams, preference.video).getVideoArguments() }
        val aArg = vaas.getAudioStream()?.let { AudioArguments(it, serializedParsedStreams, preference.audio).getAudioArguments() }

        val vaArgs = toFfmpegWorkerArguments(vArg, aArg)
        return if (vaArgs.isEmpty()) {
            MessageDataWrapper(Status.ERROR, message = "Unable to produce arguments")
        } else {
            FfmpegWorkerArgumentsCreated(
                status = Status.COMPLETED,
                inputFile = inputFile,
                entries = listOf(FfmpegWorkerArgument(
                    outputFile = outVideoFile,
                    arguments = vaArgs
                ))
            )
        }
    }

    private fun getFfmpegSubtitleArguments(
        inputFile: String,
        outDir: File,
        baseInfo: BaseInfoPerformed,
        serializedParsedStreams: ParsedMediaStreams
    ): MessageDataWrapper {
        val subRootDir = outDir.using("sub")
        val sArg = SubtitleArguments(serializedParsedStreams.subtitleStream).getSubtitleArguments()

        val entries = sArg.mapNotNull {
            FfmpegWorkerArgument(
                arguments = it.codecParameters + it.optionalParameters + listOf("-map", "0:s:${it.index}"),
                outputFile = subRootDir.using(it.language, "${baseInfo.sanitizedName}.${it.format}").absolutePath
            )
        }
        return FfmpegWorkerArgumentsCreated(
            status = Status.COMPLETED,
            inputFile = inputFile,
            entries = entries
        )
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

    private class VideoArguments(val videoStream: VideoStream, val allStreams: ParsedMediaStreams, val preference: VideoPreference) {
        fun isVideoCodecEqual() = getCodec(videoStream.codec_name) == getCodec(preference.codec.lowercase())
        protected fun getCodec(name: String): String {
            return when (name) {
                "hevc", "hevec", "h265", "h.265", "libx265"
                -> "libx265"

                "h.264", "h264", "libx264"
                -> "libx264"

                else -> name
            }
        }

        fun getVideoArguments(): VideoArgumentsDto {
            val optionalParams = mutableListOf<String>()
            if (preference.pixelFormatPassthrough.none { it == videoStream.pix_fmt }) {
                optionalParams.addAll(listOf("-pix_fmt", preference.pixelFormat))
            }
            val codecParams = if (isVideoCodecEqual()) listOf("-vcodec", "copy")
            else {
                optionalParams.addAll(listOf("-crf", preference.threshold.toString()))
                listOf("-c:v", getCodec(preference.codec.lowercase()))
            }

            return VideoArgumentsDto(
                index = allStreams.videoStream.indexOf(videoStream),
                codecParameters = codecParams,
                optionalParameters = optionalParams
            )
        }
    }

    private class AudioArguments(val audioStream: AudioStream, val allStreams: ParsedMediaStreams, val preference: AudioPreference) {
        fun isAudioCodecEqual() = audioStream.codec_name.lowercase() == preference.codec.lowercase()
        private fun shouldUseEAC3(): Boolean {
            return (preference.defaultToEAC3OnSurroundDetected && audioStream.channels > 2 && audioStream.codec_name.lowercase() != "eac3")
        }

        fun getAudioArguments(): AudioArgumentsDto {
            val optionalParams = mutableListOf<String>()
            val codecParams = if (shouldUseEAC3())
                listOf("-c:a", "eac3")
            else if (!isAudioCodecEqual()) {
                listOf("-c:a", preference.codec)
            } else
                listOf("-acodec", "copy")
            return AudioArgumentsDto(
                index = allStreams.audioStream.indexOf(audioStream),
                codecParameters = codecParams,
                optionalParameters = optionalParams
            )
        }

    }

    private class SubtitleArguments(val subtitleStreams: List<SubtitleStream>) {
        /**
         * @property DEFAULT is default subtitle as dialog
         * @property CC is Closed-Captions
         * @property SHD is Hard of hearing
         * @property NON_DIALOGUE is for Signs or Song (as in lyrics)
         */
        private enum class SubtitleType {
            DEFAULT,
            CC,
            SHD,
            NON_DIALOGUE
        }

        private fun SubtitleStream.isCC(): Boolean {
            val title = this.tags.title?.lowercase() ?: return false
            val keywords = listOf("cc", "closed caption")
            return keywords.any { title.contains(it) }
        }
        private fun SubtitleStream.isSHD(): Boolean {
            val title = this.tags.title?.lowercase() ?: return false
            val keywords = listOf("shd", "hh", "Hard-of-Hearing", "Hard of Hearing")
            return keywords.any { title.contains(it) }
        }
        private fun SubtitleStream.isSignOrSong(): Boolean {
            val title = this.tags.title?.lowercase() ?: return false
            val keywords = listOf("song", "songs", "sign", "signs")
            return keywords.any { title.contains(it) }
        }
        private fun getSubtitleType(stream: SubtitleStream): SubtitleType {
            return if (stream.isSignOrSong())
                SubtitleType.NON_DIALOGUE
            else if (stream.isSHD()) {
                SubtitleType.SHD
            } else if (stream.isCC()) {
                SubtitleType.CC
            } else SubtitleType.DEFAULT
        }

        fun getSubtitleArguments(): List<SubtitleArgumentsDto> {
            val acceptable = subtitleStreams.filter { !it.isSignOrSong() }
            val codecFiltered = acceptable.filter { getFormatToCodec(it.codec_name) != null }
            val mappedToType = codecFiltered.map { getSubtitleType(it) to it }.filter { it.first in SubtitleType.entries }
                .groupBy { it.second.tags.language ?: "eng" }
                .mapValues { entry ->
                    val languageStreams = entry.value
                    val sortedStreams = languageStreams.sortedBy { SubtitleType.entries.indexOf(it.first) }
                    sortedStreams.firstOrNull()?.second
                }.mapNotNull { it.value }

            return mappedToType.mapNotNull { stream ->
                getFormatToCodec(stream.codec_name)?.let { format ->
                    SubtitleArgumentsDto(
                        index = subtitleStreams.indexOf(stream),
                        language = stream.tags.language ?: "eng",
                        format = format
                    )
                }
            }

        }

        fun getFormatToCodec(codecName: String): String? {
            return when(codecName) {
                "ass" -> "ass"
                "subrip" -> "srt"
                "webvtt", "vtt" -> "vtt"
                "smi" -> "smi"
                "hdmv_pgs_subtitle" -> null
                else -> null
            }
        }

    }


    private fun toFfmpegWorkerArguments(
        videoArguments: VideoArgumentsDto?,
        audioArguments: AudioArgumentsDto?
    ): List<String> {
        val arguments = mutableListOf<String>(
            *videoArguments?.codecParameters?.toTypedArray() ?: arrayOf(),
            *videoArguments?.optionalParameters?.toTypedArray() ?: arrayOf(),
            *audioArguments?.codecParameters?.toTypedArray() ?: arrayOf(),
            *audioArguments?.optionalParameters?.toTypedArray() ?: arrayOf()
        )
        videoArguments?.index?.let {
            arguments.addAll(listOf("-map", "0:v:$it"))
        }
        audioArguments?.index?.let {
            arguments.addAll(listOf("-map", "0:a:$it"))
        }
        return arguments
    }
}