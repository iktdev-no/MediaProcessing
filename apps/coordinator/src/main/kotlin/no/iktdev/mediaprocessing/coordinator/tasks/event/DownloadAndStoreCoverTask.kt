package no.iktdev.mediaprocessing.coordinator.tasks.event

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
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
class DownloadAndStoreCoverTask(@Autowired override var coordinator: EventCoordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}

    val serviceId = "${getComputername()}::${this.javaClass.simpleName}::${UUID.randomUUID()}"
    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventWorkDownloadCoverPerformed

    override val requiredEvents: List<KafkaEvents>
        get() = listOf(
            KafkaEvents.EventMediaMetadataSearchPerformed,
            KafkaEvents.EventMediaReadOutCover,
            KafkaEvents.EventWorkEncodePerformed
        )
    override fun prerequisitesRequired(events: List<PersistentMessage>): List<() -> Boolean> {
        return super.prerequisitesRequired(events) + listOf {
            isPrerequisiteDataPresent(events)
        }
    }

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)

        log.info { "${event.referenceId} triggered by ${event.event}" }

        val cover = events.find { it.event == KafkaEvents.EventMediaReadOutCover }
        if (cover == null || cover.data !is CoverInfoPerformed) {
            return SimpleMessageData(Status.ERROR, "Wrong type triggered and caused an execution for $serviceId", event.eventId)
        }
        val coverData = cover.data as CoverInfoPerformed
        val outDir = File(coverData.outDir)
        if (!outDir.exists())
            return SimpleMessageData(Status.ERROR, "Check for output directory for cover storage failed for $serviceId", event.eventId)

        val client = DownloadClient(coverData.url, File(coverData.outDir), coverData.outFileBaseName)

        val outFile = runBlocking {
            client.getOutFile()
        }

        val coversInDifferentFormats = outDir.listFiles { it -> it.isFile && it.extension.lowercase() in client.contentTypeToExtension().values } ?: emptyArray()


        var message: String? = null
        var status = Status.COMPLETED
        val result = if (outFile?.exists() == true) {
            message = "${outFile.name} already exists"
            status = Status.SKIPPED
            outFile
        } else if (coversInDifferentFormats.isNotEmpty()) {
            status = Status.SKIPPED
            coversInDifferentFormats.random()
        } else if (outFile != null) {
            runBlocking {
                client.download(outFile)
            }
        } else {
            null
        }

        return if (result == null) {
            SimpleMessageData(Status.ERROR, "Could not download cover, check logs", event.eventId)
        } else {
            if (!result.exists() || !result.canRead()) {
                status = Status.ERROR
            }
            CoverDownloadWorkPerformed(status = status, message = message, coverFile = result.absolutePath, event.eventId)
        }
    }
}