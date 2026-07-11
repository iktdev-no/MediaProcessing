package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.PersistContentEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.TransferredContentsSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events_super.TransferredBaseResultEvent
import no.iktdev.mediaprocessing.shared.common.listeners.SummaryEventListener
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.TaskProjection
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Component

@Component
class TransferredContentsEventsListener(val eventStore: no.iktdev.eventi.stores.EventStore = EventStore): SummaryEventListener(eventStore) {
    private val log = KotlinLogging.logger {}

    val requiredTransferStatus = listOf(
        CollectProjection.TaskStatus.Skipped,
        CollectProjection.TaskStatus.Completed
    )

    override fun shouldSummarize(fullHistory: List<Event>): Boolean {
        val transferStatus = TaskProjection(fullHistory)
        if (transferStatus.projectMigrateContentStatus() !in requiredTransferStatus) {
            return false
        }
        return true
    }

    override fun produceSummary(fullHistory: List<Event>): Event {
        // 1. Hent ut ID-ene til alle fil-eventene som er med i denne beregningen akkurat nå
        // (Her må du filtrere på de event-typene som TaskProjection faktisk bruker)
        val targetEventIds = fullHistory
            .filter { it is TransferredBaseResultEvent || it is PersistContentEvent }
            .map { it.eventId }
            .toSet()

        // 2. Returner det nye samle-eventet som holder på disse ID-ene
        return TransferredContentsSummaryEvent(
            summarizedEventIds = targetEventIds
        ).derivedOf(fullHistory.last())
    }

    override fun summaryAlreadyExists(fullHistory: List<Event>): Boolean {
        // 1. Finn alle relevante fil-events i historikken
        val currentFileEventIds = fullHistory
            .filter { it is TransferredBaseResultEvent || it is PersistContentEvent }
            .map { it.eventId }
            .toSet()

        if (currentFileEventIds.isEmpty()) return false

        // 2. Sjekk om det allerede finnes et summary-event som dekker NØYAKTIG denne tilstanden
        val existingSummary = fullHistory
            .filterIsInstance<TransferredContentsSummaryEvent>()
            .firstOrNull { it.summarizedEventIds == currentFileEventIds }

        // Hvis vi fant en eksisterende summary med nøyaktig de samme event-ID-ene,
        // så betyr det at vi ALLEREDE har produsert et event for akkurat denne tilstanden.
        // Returner true for å ignorere (ejecte).
        return existingSummary != null
    }
}