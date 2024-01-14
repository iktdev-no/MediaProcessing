package no.iktdev.mediaprocessing.coordinator.tasks.event

import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.CoverInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MetadataPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.VideoInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MetadataAndBaseInfoToCoverTask(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {

    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_MEDIA_READ_OUT_COVER

    override val requiredEvents: List<KafkaEvents> = listOf(
        KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED,
        KafkaEvents.EVENT_MEDIA_READ_OUT_NAME_AND_TYPE,
        KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED
    )

    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        log.info { "${this.javaClass.simpleName} triggered by ${event.event}" }

        val baseInfo = events.findLast { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed
        val meta = events.findLast { it.data is MetadataPerformed }?.data as MetadataPerformed? ?: return null
        val fileOut = events.findLast { it.data is VideoInfoPerformed }?.data as VideoInfoPerformed? ?: return null

        val coverUrl = meta?.data?.cover
        return if (coverUrl.isNullOrBlank()) {
            log.warn { "No cover available for ${baseInfo.title}" }
            null
        } else {
            CoverInfoPerformed(
                status = Status.COMPLETED,
                url = coverUrl,
                outFileBaseName = baseInfo.title,
                outDir = fileOut.outDirectory
            )
        }
    }
}