package no.iktdev.mediaprocessing.coordinator.tasksV2.implementations

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.referenceId
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
        assert(events.isNotEmpty()) {
            "Events are nor present"
        }
        val startEvent = events.find { it.eventType == Events.ProcessStarted }?.az<MediaProcessStartEvent>()
        if (startEvent == null) {
            log.error { "Start event not found on ${incomingEvent.referenceId()}." }
            try {
                log.error { WGson.toJson(startEvent) }
                log.warn { "EvenTypes:\n" + events.map { it.eventType }.map { "\n\t$it" } }
                log.warn { "Events provided:\n ${WGson.toJson(events)}" }
            } catch (e: Exception) {}
            return false
        }


        val startType = startEvent?.data?.type
        if (startEvent == null) {
            log.error { "Start type on ${incomingEvent.referenceId()}. Requiring permit event" }
            try {
                log.error { WGson.toJson(startEvent) }
            } catch (e: Exception) {}
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