package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.datasource.toEpochSeconds
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameDeterminate
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MetadataPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.VideoInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.hasValidData
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
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
class MetadataAndBaseInfoToFileOut(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}

    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_MEDIA_READ_OUT_NAME_AND_TYPE

    val waitingProcessesForMeta: MutableMap<String, LocalDateTime> = mutableMapOf()

    override val listensForEvents: List<KafkaEvents> = listOf(
        KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED,
        KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED
    )

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        log.info { "${this.javaClass.simpleName} triggered by ${event.event}" }

        val baseInfo = events.lastOrSuccessOf(KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED) { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed?
        val meta = events.lastOrSuccessOf(KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED) { it.data is MetadataPerformed }?.data as MetadataPerformed?

        // Only Return here as both baseInfo events are required to continue
        if (!baseInfo.isSuccess() || !baseInfo.hasValidData() || events.any { it.event == KafkaEvents.EVENT_MEDIA_READ_OUT_NAME_AND_TYPE }) {
            return null
        }
        if (baseInfo.isSuccess() && meta == null) {
            log.info { "Sending ${baseInfo?.title} to waiting queue" }
            if (!waitingProcessesForMeta.containsKey(event.referenceId)) {
                waitingProcessesForMeta[event.referenceId] = LocalDateTime.now()
            }
            return null
        }

        if (!isPrerequisiteDataPresent(events)) {
            return null
        }

        baseInfo ?: return null // Return if baseInfo is null

        val metaContentType: String? = if (meta.isSuccess()) meta?.data?.type else null
        val contentType = when (metaContentType) {
            "serie", "tv" -> FileNameDeterminate.ContentType.SERIE
            "movie" -> FileNameDeterminate.ContentType.MOVIE
            else -> FileNameDeterminate.ContentType.UNDEFINED
        }

        val fileDeterminate = FileNameDeterminate(baseInfo.title, baseInfo.sanitizedName, contentType)
        if (waitingProcessesForMeta.containsKey(event.referenceId)) {
            waitingProcessesForMeta.remove(event.referenceId)
        }

        val outputDirectory = SharedConfig.outgoingContent.using(baseInfo.title)

        val vi = fileDeterminate.getDeterminedVideoInfo()?.toJsonObject()
        return if (vi != null) {
            VideoInfoPerformed(Status.COMPLETED, vi, outDirectory = outputDirectory.absolutePath)
        } else {
            MessageDataWrapper(Status.ERROR, "No VideoInfo found...")
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
            producer.sendMessage(it.key, KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED, MetadataPerformed(status = Status.ERROR, "Timed Out by: ${this@MetadataAndBaseInfoToFileOut::class.simpleName}"))
            waitingProcessesForMeta.remove(it.key)
        }
    }

}