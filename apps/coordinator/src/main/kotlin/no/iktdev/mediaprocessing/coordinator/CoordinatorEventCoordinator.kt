package no.iktdev.mediaprocessing.coordinator

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.contract.data.MediaProcessStartEvent
import no.iktdev.mediaprocessing.shared.contract.data.PermitWorkCreationEvent
import no.iktdev.mediaprocessing.shared.contract.data.StartEventData
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.io.File
import java.util.*

@Component
class Coordinator(
    @Autowired
    override var applicationContext: ApplicationContext,
    @Autowired
    override var eventManager: EventsManager

) : EventCoordinator<Event, EventsManager>() {

    init {
    }

    public fun startProcess(file: File, type: ProcessType) {
        val operations: List<StartOperationEvents> = listOf(
            StartOperationEvents.ENCODE,
            StartOperationEvents.EXTRACT,
            StartOperationEvents.CONVERT
        )
        startProcess(file, type, operations)
    }

    fun startProcess(file: File, type: ProcessType, operations: List<StartOperationEvents>): UUID {
        val referenceId: UUID = UUID.randomUUID()
        val event = MediaProcessStartEvent(
            metadata = EventMetadata(
                referenceId = referenceId.toString(),
                status = EventStatus.Success
            ),
            data = StartEventData(
                file = file.absolutePath,
                type = type,
                operations = operations
            )
        )

        produceNewEvent(event)
        return referenceId
    }

    fun permitWorkToProceedOn(referenceId: String, events: List<Event>, message: String) {
        val defaultRequiredBy = listOf(Events.EventMediaParameterEncodeCreated, Events.EventMediaParameterExtractCreated)
        val eventToAttachTo = if (events.any { it.eventType in defaultRequiredBy }) {
            events.findLast { it.eventType in defaultRequiredBy }
        } else events.find { it.eventType == Events.EventMediaProcessStarted }
        if (eventToAttachTo == null) {
            log.error { "No event to attach permit to" }
            return
        }


        produceNewEvent(PermitWorkCreationEvent(
            metadata = EventMetadata(
                referenceId = referenceId,
                derivedFromEventId = eventToAttachTo.eventId(),
                status = EventStatus.Success
            ),
            data = message
        ))
    }
}
