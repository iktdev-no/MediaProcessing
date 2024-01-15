package no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg

import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.Preference
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.*
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class EncodeArgumentCreatorTask(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}

    val preference = Preference.getPreference()
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED

    override val requiredEvents: List<KafkaEvents> =
        listOf(
            KafkaEvents.EVENT_PROCESS_STARTED,
            KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED,
            KafkaEvents.EVENT_MEDIA_PARSE_STREAM_PERFORMED,
            KafkaEvents.EVENT_MEDIA_READ_OUT_NAME_AND_TYPE
        )

    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        log.info { "${this.javaClass.simpleName} triggered by ${event.event}" }

        val inputFile = events.find { it.data is ProcessStarted }?.data as ProcessStarted
        val baseInfo = events.findLast { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed
        val readStreamsEvent = events.find { it.data is MediaStreamsParsePerformed }?.data as MediaStreamsParsePerformed
        val serializedParsedStreams = readStreamsEvent.streams

        val outDir = SharedConfig.outgoingContent.using(baseInfo.title)

        return getFfmpegVideoArguments(
            inputFile = inputFile.file,
            outDir = outDir,
            preference = preference.encodePreference,
            baseInfo = baseInfo,
            serializedParsedStreams = serializedParsedStreams
        )
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

        val vArg = vaas.getVideoStream()
            ?.let { VideoArguments(it, serializedParsedStreams, preference.video).getVideoArguments() }
        val aArg = vaas.getAudioStream()
            ?.let { AudioArguments(it, serializedParsedStreams, preference.audio).getAudioArguments() }

        val vaArgs = toFfmpegWorkerArguments(vArg, aArg)
        return if (vaArgs.isEmpty()) {
            MessageDataWrapper(Status.ERROR, message = "Unable to produce arguments")
        } else {
            FfmpegWorkerArgumentsCreated(
                status = Status.COMPLETED,
                inputFile = inputFile,
                entries = listOf(
                    FfmpegWorkerArgument(
                        outputFile = outVideoFile,
                        arguments = vaArgs
                    )
                )
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

    private class VideoArguments(
        val videoStream: VideoStream,
        val allStreams: ParsedMediaStreams,
        val preference: VideoPreference
    ) {
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

    private class AudioArguments(
        val audioStream: AudioStream,
        val allStreams: ParsedMediaStreams,
        val preference: AudioPreference
    ) {
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
}