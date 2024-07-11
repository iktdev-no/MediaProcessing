package no.iktdev.mediaprocessing.coordinator.tasksV2.implementations

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsListenerContract
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.contract.data.MediaProcessStartEvent
import no.iktdev.mediaprocessing.shared.contract.data.az

abstract class WorkTaskListener: CoordinatorEventListener() {
    private val log = KotlinLogging.logger {}

    fun canStart(incomingEvent: Event, events: List<Event>): Boolean {
        val autoStart = events.find { it.eventType == Events.EventMediaProcessStarted }?.az<MediaProcessStartEvent>()?.data
        if (autoStart == null) {
            log.error { "Start event not found. Requiring permitt event" }
        }
        return if (incomingEvent.eventType == Events.EventMediaWorkProceedPermitted) {
            return true
        } else {
            if (autoStart == null || autoStart.type == ProcessType.MANUAL) {
                log.warn { "${incomingEvent.metadata.referenceId} waiting for Proceed event due to Manual process" }
                false
            } else true
        }
    }
}