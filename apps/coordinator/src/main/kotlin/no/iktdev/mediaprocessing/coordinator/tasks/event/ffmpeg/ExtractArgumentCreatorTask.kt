package no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg

import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg.ExtractArgumentCreatorTask.SubtitleArguments.SubtitleType.*
import no.iktdev.mediaprocessing.shared.common.Preference
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.SubtitleArgumentsDto
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.SubtitleStream
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ExtractArgumentCreatorTask(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}

    val preference = Preference.getPreference()

    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED

    override val requiredEvents: List<KafkaEvents> = listOf(
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
        if (!requiredEvents.contains(event.event)) {
            log.info { "${this.javaClass.simpleName} ignores ${event.event}@${event.eventId}" }
            return null
        }

        val inputFile = events.find { it.data is ProcessStarted }?.data as ProcessStarted
        val baseInfo = events.findLast { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed
        val readStreamsEvent = events.find { it.data is MediaStreamsParsePerformed }?.data as MediaStreamsParsePerformed
        val serializedParsedStreams = readStreamsEvent.streams

        val outDir = SharedConfig.outgoingContent.using(baseInfo.title)

        return getFfmpegSubtitleArguments(
            inputFile = inputFile.file,
            outDir = outDir,
            baseInfo = baseInfo,
            serializedParsedStreams = serializedParsedStreams
        )
    }

    private fun getFfmpegSubtitleArguments(
        inputFile: String,
        outDir: File,
        baseInfo: BaseInfoPerformed,
        serializedParsedStreams: ParsedMediaStreams
    ): MessageDataWrapper {
        val subRootDir = outDir.using("sub")
        val sArg = SubtitleArguments(serializedParsedStreams.subtitleStream).getSubtitleArguments()

        val entries = sArg.map {
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
            val mappedToType =
                codecFiltered.map { getSubtitleType(it) to it }.filter { it.first in SubtitleType.entries }
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
            return when (codecName) {
                "ass" -> "ass"
                "subrip" -> "srt"
                "webvtt", "vtt" -> "vtt"
                "smi" -> "smi"
                "hdmv_pgs_subtitle" -> null
                else -> null
            }
        }

    }


}