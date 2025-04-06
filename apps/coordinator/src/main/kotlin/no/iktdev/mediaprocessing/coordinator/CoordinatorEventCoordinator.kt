package no.iktdev.mediaprocessing.coordinator

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.implementations.ActiveMode
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.ProcessType
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import no.iktdev.mediaprocessing.shared.common.contract.data.MediaProcessStartEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.PermitWorkCreationEvent
import no.iktdev.mediaprocessing.shared.common.contract.data.StartEventData
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import no.iktdev.mediaprocessing.shared.common.database.cal.EventsManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.event.EventListener
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

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        onReady()
    }

    fun getProducerName(): String {
        return this::class.java.simpleName
    }

    public fun startProcess(file: File, type: ProcessType) {
        val operations: List<OperationEvents> = listOf(
            OperationEvents.ENCODE,
            OperationEvents.EXTRACT,
            OperationEvents.CONVERT
        )
        startProcess(file, type, operations)
    }

    fun startProcess(file: File, type: ProcessType, operations: List<OperationEvents>): UUID {
        val referenceId: UUID = UUID.randomUUID()
        val event = MediaProcessStartEvent(
            metadata = EventMetadata(
                referenceId = referenceId.toString(),
                status = EventStatus.Success,
                source = getProducerName()
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
        val defaultRequiredBy = listOf(Events.EncodeParameterCreated, Events.ExtractParameterCreated)
        val eventToAttachTo = if (events.any { it.eventType in defaultRequiredBy }) {
            events.findLast { it.eventType in defaultRequiredBy }
        } else events.find { it.eventType == Events.ProcessStarted }
        if (eventToAttachTo == null) {
            log.error { "No event to attach permit to" }
            return
        }


        produceNewEvent(
            PermitWorkCreationEvent(
                metadata = EventMetadata(
                    referenceId = referenceId,
                    derivedFromEventId = eventToAttachTo.eventId(),
                    status = EventStatus.Success,
                    source = getProducerName()
                ),
                data = message
            )
        )
    }

    override fun getActiveTaskMode(): ActiveMode {
        if (runnerManager.iAmSuperseded()) {
            // This will let the application complete but not consume new
            taskMode = ActiveMode.Passive
        }
        return taskMode
    }
}
