package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.derivedFromEventId
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.data.referenceId
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.taskManager
import no.iktdev.mediaprocessing.coordinator.tasksV2.implementations.WorkTaskListener
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtractWorkTaskListener: WorkTaskListener() {
    private val log = KotlinLogging.logger {}

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.EventWorkExtractCreated
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaParameterExtractCreated,
        Events.EventMediaWorkProceedPermitted
    )

    override fun canProduceMultipleEvents(): Boolean {
        return true
    }

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val state =  super.shouldIProcessAndHandleEvent(incomingEvent, events)
        return state
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }

        if (!canStart(event, events)) {
            return
        }

        val arguments = if (event.eventType == Events.EventMediaParameterExtractCreated) {
            event.az<ExtractArgumentCreatedEvent>()?.data
        } else {
            events.find { it.eventType == Events.EventMediaParameterExtractCreated }
                ?.az<ExtractArgumentCreatedEvent>()?.data
        }
        if (arguments == null) {
            log.error { "No Extract arguments found.. referenceId: ${event.referenceId()}" }
            return
        }
        if (arguments.isEmpty()) {
            ExtractWorkCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Failed)
            )
            return
        }

        arguments.mapNotNull {
            ExtractWorkCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success),
                data = it
            )
        }.forEach { event ->
            onProduceEvent(event)
            taskManager.createTask(
                referenceId = event.referenceId(),
                eventId = event.eventId(),
                derivedFromEventId = event.derivedFromEventId(),
                task = TaskType.Extract,
                data = WGson.gson.toJson(event.data!!),
                inputFile = event.data!!.inputFile
            )
        }
    }
}