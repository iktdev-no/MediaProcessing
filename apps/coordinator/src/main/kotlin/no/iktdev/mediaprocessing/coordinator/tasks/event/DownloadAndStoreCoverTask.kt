package no.iktdev.mediaprocessing.coordinator.tasks.event

import kotlinx.coroutines.runBlocking
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.DownloadClient
import no.iktdev.mediaprocessing.shared.common.getComputername
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.CoverDownloadWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.CoverInfoPerformed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.util.*

@Service
class DownloadAndStoreCoverTask(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    val serviceId = "${getComputername()}::${this.javaClass.simpleName}::${UUID.randomUUID()}"
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EVENT_WORK_DOWNLOAD_COVER_PERFORMED

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(
            KafkaEvents.EVENT_MEDIA_METADATA_SEARCH_PERFORMED,
            KafkaEvents.EVENT_MEDIA_READ_OUT_COVER,
            KafkaEvents.EVENT_WORK_ENCODE_PERFORMED
        )
    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        val cover = events.find { it.event == KafkaEvents.EVENT_MEDIA_READ_OUT_COVER }
        if (cover == null || cover.data !is CoverInfoPerformed) {
            return SimpleMessageData(Status.ERROR, "Wrong type triggered and caused an execution for $serviceId")
        }
        val coverData = cover.data as CoverInfoPerformed
        val outDir = File(coverData.outDir)
        if (!outDir.exists())
            return SimpleMessageData(Status.ERROR, "Check for output directory for cover storage failed for $serviceId")

        val client = DownloadClient(coverData.url, File(coverData.outDir), coverData.outFileBaseName)
        val result = runBlocking {
            client.download()
        }
        return if (result == null) {
            SimpleMessageData(Status.ERROR, "Could not download cover, check logs")
        } else {
            val status = if (result.exists() && result.canRead()) Status.COMPLETED else Status.ERROR
            CoverDownloadWorkPerformed(status = status, coverFile = result.absolutePath)
        }
    }
}