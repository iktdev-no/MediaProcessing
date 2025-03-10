package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.derivedFromEventId
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.data.referenceId
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.taskManager
import no.iktdev.mediaprocessing.coordinator.tasksV2.implementations.WorkTaskListener
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EncodeWorkTaskListener : WorkTaskListener() {
    private val log = KotlinLogging.logger {}

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.EncodeTaskCreated
    override val listensForEvents: List<Events> = listOf(
        Events.EncodeParameterCreated,
        Events.WorkProceedPermitted
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
        active = true

        val encodeArguments = if (event.eventType == Events.EncodeParameterCreated) {
            event.az<EncodeArgumentCreatedEvent>()?.data
        } else {
            events.find { it.eventType == Events.EncodeParameterCreated }
                ?.az<EncodeArgumentCreatedEvent>()?.data
        }
        if (encodeArguments == null) {
            log.error { "No Encode arguments found.. referenceId: ${event.referenceId()}" }
            active = false
            return
        }
        EncodeWorkCreatedEvent(
            metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
            data = encodeArguments
        ).also { event ->
            val taskCreatedSuccessfully = taskManager.createTask(
                referenceId = event.referenceId(),
                eventId = event.eventId(),
                derivedFromEventId = event.derivedFromEventId(),
                task = TaskType.Encode,
                data = WGson.gson.toJson(event.data!!),
                inputFile = event.data!!.inputFile
            )
            if (!taskCreatedSuccessfully) {
                log.error { "Failed to create task for events on referenceId: ${event.referenceId()}" }
            } else {
                onProduceEvent(event)
            }
        }
        active = false
    }
}