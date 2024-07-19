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
class EncodeWorkTaskListener : WorkTaskListener() {
    private val log = KotlinLogging.logger {}

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.EventWorkEncodeCreated
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaParameterEncodeCreated,
        Events.EventMediaWorkProceedPermitted
    )

    override fun canProduceMultipleEvents(): Boolean {
        return true
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

        val encodeArguments = if (event.eventType == Events.EventMediaParameterEncodeCreated) {
            event.az<EncodeArgumentCreatedEvent>()?.data
        } else {
            events.find { it.eventType == Events.EventMediaParameterEncodeCreated }
                ?.az<EncodeArgumentCreatedEvent>()?.data
        }
        if (encodeArguments == null) {
            log.error { "No Encode arguments found.. referenceId: ${event.referenceId()}" }
            return
        }
        EncodeWorkCreatedEvent(
            metadata = event.makeDerivedEventInfo(EventStatus.Success),
            data = encodeArguments
        ).also { event ->
            onProduceEvent(event)
            taskManager.createTask(
                referenceId = event.referenceId(),
                eventId = event.eventId(),
                derivedFromEventId = event.derivedFromEventId(),
                task = TaskType.Encode,
                data = WGson.gson.toJson(event.data!!),
                inputFile = event.data!!.inputFile
            )
        }

    }
}