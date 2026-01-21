package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import org.springframework.stereotype.Component

@Component
class CollectEventsListener: EventListener() {
    private val log = KotlinLogging.logger {}

    val undesiredStates = listOf(CollectProjection.TaskStatus.Failed, CollectProjection.TaskStatus.Pending)
    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {

        val collectProjection = CollectProjection(history)
        log.info { collectProjection.prettyPrint() }

        val taskStatus = collectProjection.getTaskStatus()
        if (taskStatus.all { it == CollectProjection.TaskStatus.NotInitiated }) {
            // No work has been done, so we are not ready
            return null
        }
        val statusAcceptable = taskStatus.none { it in undesiredStates }
        if (!statusAcceptable) {
            if (taskStatus.any { it == CollectProjection.TaskStatus.Failed }) {
                log.warn { "One or more tasks have failed in  ${event.referenceId}" }
            } else {
                log.info { "One or more tasks are still pending in  ${event.referenceId}" }
            }
            return null
        }

        return CollectedEvent(history.map { it.eventId }.toSet()).derivedOf(event)
    }
}