package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.shared.common.DownloadClient
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsListenerContract
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class CoverDownloadTaskListener : CoordinatorEventListener() {
    val log = KotlinLogging.logger {}

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.EventWorkDownloadCoverPerformed
    override val listensForEvents: List<Events> = listOf(Events.EventMediaReadOutCover)
    override fun onEventsReceived(incomingEvent: Event, events: List<Event>) {
        val failedEventDefault = MediaCoverDownloadedEvent(
            metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed)
        )

        val data = incomingEvent.az<MediaCoverInfoReceivedEvent>()?.data
        if (data == null) {
            log.error { "No valid data for use to obtain cover" }
            onProduceEvent(failedEventDefault)
            return
        }

        val outDir = File(data.outDir)
        if (!outDir.exists()) {
            log.error { "Check for output directory for cover storage failed for ${incomingEvent.metadata.eventId} " }
            onProduceEvent(failedEventDefault)
        }

        val client = DownloadClient(data.url, File(data.outDir), data.outFileBaseName)

        val outFile = runBlocking {
            client.getOutFile()
        }

        val coversInDifferentFormats = outDir.listFiles { it -> it.isFile && it.extension.lowercase() in client.contentTypeToExtension().values } ?: emptyArray()

        val result = if (outFile?.exists() == true) {
            outFile
        } else if (coversInDifferentFormats.isNotEmpty()) {
            coversInDifferentFormats.random()
        } else if (outFile != null) {
            runBlocking {
                client.download(outFile)
            }
        } else {
            null
        }

        if (result == null) {
            log.error { "Could not download cover, check logs ${incomingEvent.metadata.eventId} " }
        } else {
            if (!result.exists() || !result.canRead()) {
                onProduceEvent(failedEventDefault)
                return
            }
            onProduceEvent(MediaCoverDownloadedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Success),
                data = DownloadedCover(result.absolutePath)
            ))
        }

    }
}