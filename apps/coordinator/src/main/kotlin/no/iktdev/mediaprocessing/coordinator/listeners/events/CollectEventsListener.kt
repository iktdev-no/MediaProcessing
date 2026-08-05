package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.listeners.SummaryEventListener
import no.iktdev.mediaprocessing.shared.common.projection.WorkflowProjection
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import org.springframework.stereotype.Component

@Component
class CollectEventsListener(eventStore: no.iktdev.eventi.stores.EventStore = EventStore) : SummaryEventListener(eventStore) {

    private val log = KotlinLogging.logger {}
    override fun shouldSummarize(effectiveHistory: List<Event>): Boolean {
        val workflow = WorkflowProjection(effectiveHistory)
        val report = workflow.evaluate()

        if (report.isFailed()) {
            val referenceId = effectiveHistory.firstOrNull()?.referenceId ?: "unknown"
            log.warn { "Workflow failed or incomplete for referenceId=$referenceId with reason: ${report.reason}" }
            return false
        }

        return workflow.isWorkflowComplete()
    }

    override fun produceSummary(effectiveHistory: List<Event>): Event {
        // Must have all relevant tasks completed
        val eventIds = effectiveHistory.map { it.eventId }.toSet()

        return CollectedEvent(eventIds).derivedOf(effectiveHistory.last())
    }

    override fun summaryAlreadyExists(effectiveHistory: List<Event>): Boolean {
        return effectiveHistory.any { it is CollectedEvent }
    }
}

