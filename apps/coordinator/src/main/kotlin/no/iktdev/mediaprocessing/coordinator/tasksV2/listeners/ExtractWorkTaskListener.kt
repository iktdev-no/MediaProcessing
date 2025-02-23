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
class ExtractWorkTaskListener: WorkTaskListener() {
    private val log = KotlinLogging.logger {}

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.WorkExtractCreated
    override val listensForEvents: List<Events> = listOf(
        Events.ParameterExtractCreated,
        Events.WorkProceedPermitted
    )

    override fun canProduceMultipleEvents(): Boolean {
        return true
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            active = false
            return
        }
        active = true

        val arguments = if (event.eventType == Events.ParameterExtractCreated) {
            event.az<ExtractArgumentCreatedEvent>()?.data
        } else {
            events.find { it.eventType == Events.ParameterExtractCreated }
                ?.az<ExtractArgumentCreatedEvent>()?.data
        }
        if (arguments == null) {
            log.error { "No Extract arguments found.. referenceId: ${event.referenceId()}" }
            active = false
            return
        }
        if (arguments.isEmpty()) {
            active = false
            return
        }

        arguments.mapNotNull {
            ExtractWorkCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
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
        active = false
    }
}