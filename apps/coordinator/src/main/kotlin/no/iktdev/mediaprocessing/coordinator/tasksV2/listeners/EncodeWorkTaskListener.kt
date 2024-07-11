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
class EncodeWorkTaskListener : WorkTaskListener() {
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

        val encodeArguments = if (incomingEvent.eventType == Events.EventMediaParameterEncodeCreated) {
            incomingEvent.az<EncodeArgumentCreatedEvent>()?.data
        } else {
            events.find { it.eventType == Events.EventMediaParameterEncodeCreated }
                ?.az<EncodeArgumentCreatedEvent>()?.data
        }
        if (encodeArguments == null) {
            log.error { "No Encode arguments found.. referenceId: ${incomingEvent.referenceId()}" }
            return
        }

        onProduceEvent(
            EncodeWorkCreatedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Success),
                data = encodeArguments
            )
        )
    }
}