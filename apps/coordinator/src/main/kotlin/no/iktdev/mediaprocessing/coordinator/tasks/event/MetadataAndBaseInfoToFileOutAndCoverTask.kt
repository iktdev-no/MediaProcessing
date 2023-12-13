package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.datasource.toEpochSeconds
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameDeterminate
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 *
 */
@Service
@EnableScheduling
class MetadataAndBaseInfoToFileOutAndCoverTask(@Autowired var coordinator: Coordinator): TaskCreator() {
    private val log = KotlinLogging.logger {}
    init {
        coordinator.addListener(this)
    }
    val waitingProcessesForMeta: MutableMap<String, LocalDateTime> = mutableMapOf()


    override fun onEventReceived(referenceId: String, event: PersistentMessage, events: List<PersistentMessage>) {
        if (!listOf(KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED, KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED).contains(event.event)) {
            return
        }

        val baseInfo = events.findLast { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed?
        val meta = events.findLast { it.data is MetadataPerformed }?.data as MetadataPerformed?

        // Only Return here as both baseInfo events are required to continue
        if (!baseInfo.isSuccess() || !baseInfo.hasValidData() || events.any { it.event == KafkaEvents.EVENT_MEDIA_READ_OUT_NAME_AND_TYPE }) {
            return
        }
        if (baseInfo.isSuccess() && meta == null) {
            log.info { "Sending ${baseInfo?.title} to waiting queue" }
            if (!waitingProcessesForMeta.containsKey(referenceId)) {
                waitingProcessesForMeta[referenceId] = LocalDateTime.now()
            }
            return
        }

        baseInfo ?: return // Return if baseInfo is null

        val metaContentType: String? = if (meta.isSuccess()) meta?.data?.type else null
        val contentType = when (metaContentType) {
            "serie", "tv" -> FileNameDeterminate.ContentType.SERIE
            "movie" -> FileNameDeterminate.ContentType.MOVIE
            else -> FileNameDeterminate.ContentType.UNDEFINED
        }

        val fileDeterminate = FileNameDeterminate(baseInfo.title, baseInfo.sanitizedName, contentType)
        if (waitingProcessesForMeta.containsKey(referenceId)) {
            waitingProcessesForMeta.remove(referenceId)
        }

        val outputDirectory = SharedConfig.outgoingContent.using(baseInfo.title)

        val vi = fileDeterminate.getDeterminedVideoInfo()
        if (vi != null) {
            producer.sendMessage(
                referenceId,
                KafkaEvents.EVENT_MEDIA_READ_OUT_NAME_AND_TYPE,
                data = VideoInfoPerformed(Status.COMPLETED, vi)
            )
        } else {
            producer.sendMessage(
                referenceId,
                KafkaEvents.EVENT_MEDIA_READ_OUT_NAME_AND_TYPE,
                data = MessageDataWrapper(Status.ERROR, "No VideoInfo found...")
            )
        }


        val coverUrl = meta?.data?.cover
        if (coverUrl.isNullOrBlank()) {
            log.warn { "No cover available for ${baseInfo.title}" }
        } else {
            producer.sendMessage(
                referenceId,
                KafkaEvents.EVENT_MEDIA_DOWNLOAD_COVER_PARAMETER_CREATED,
                CoverInfoPerformed(
                    status = Status.COMPLETED,
                    url = coverUrl,
                    outFileBaseName = baseInfo.title,
                    outDir = outputDirectory.absolutePath
                )
            )
        }

    }

    //@Scheduled(fixedDelay = (60_000))
    @Scheduled(fixedDelay = (1_000))
    fun sendErrorMessageForMetadata() {
        val expired = waitingProcessesForMeta.filter {
            LocalDateTime.now().toEpochSeconds() > (it.value.toEpochSeconds() + KafkaEnv.metadataTimeoutMinutes * 60)
        }
        expired.forEach {
            log.info { "Producing timeout for ${it.key} ${LocalDateTime.now()}" }
            producer.sendMessage(it.key, KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED, MetadataPerformed(status = Status.ERROR, "Timed Out by: ${this@MetadataAndBaseInfoToFileOutAndCoverTask::class.simpleName}"))
            waitingProcessesForMeta.remove(it.key)
        }
    }

}