package no.iktdev.mediaprocessing.coordinator.tasksV2.implementations

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.ProcessType
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaProcessStartEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.az

abstract class WorkTaskListener: CoordinatorEventListener() {
    private val log = KotlinLogging.logger {}

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        return canStart(incomingEvent, events)
    }

    fun canStart(incomingEvent: Event, events: List<Event>): Boolean {
        val startEvent = events.find { it.eventType == Events.ProcessStarted }?.az<MediaProcessStartEvent>()
        val startType = startEvent?.data?.type
        if (startType == null) {
            log.error { "Start event not found. Requiring permitt event" }
            log.error { Gson().toJson(events) }
            return false
        }
        return if (incomingEvent.eventType == Events.WorkProceedPermitted) {
            return true
        } else {
            if (startType == ProcessType.MANUAL) {
                log.warn { "${incomingEvent.metadata.referenceId} waiting for Proceed event due to Manual process" }
                false
            } else true
        }
    }
}