package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.tasksV2.implementations.WorkTaskListener
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
    override val produceEvent: Events = Events.EventWorkEncodeCreated
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaParameterEncodeCreated,
        Events.EventMediaWorkProceedPermitted
    )

    override fun onEventsReceived(incomingEvent: Event, events: List<Event>) {
        if (!canStart(incomingEvent, events)) {
            return
        }

        val arguments = if (incomingEvent.eventType == Events.EventMediaParameterExtractCreated) {
            incomingEvent.az<ExtractArgumentCreatedEvent>()?.data
        } else {
            events.find { it.eventType == Events.EventMediaParameterExtractCreated }
                ?.az<ExtractArgumentCreatedEvent>()?.data
        }
        if (arguments == null) {
            log.error { "No Extract arguments found.. referenceId: ${incomingEvent.referenceId()}" }
            return
        }
        if (arguments.isEmpty()) {
            ExtractWorkCreatedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed)
            )
            return
        }

        arguments.mapNotNull {
            ExtractWorkCreatedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Success),
                data = it
            )
        }.forEach { event ->
            onProduceEvent(event)
        }
    }
}