package no.iktdev.eventi.mock.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.eventi.implementations.EventListenerImpl
import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.mock.MockDataEventListener
import no.iktdev.eventi.mock.MockEventCoordinator
import no.iktdev.eventi.mock.data.SecondEvent
import no.iktdev.eventi.mock.data.ThirdEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ThirdEventListener() : MockDataEventListener() {
    @Autowired
    override var coordinator: MockEventCoordinator? = null

    private val log = KotlinLogging.logger {}

    init {
        log.info { "Created Service: ${this::class.java.simpleName}" }
    }

    override val produceEvent = this::class.java.simpleName
    override val listensForEvents = listOf(SecondEventListener::class.java.simpleName)


    override fun  onProduceEvent(event: EventImpl) {
        super.onProduceEvent(event)
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<EventImpl>, events: List<EventImpl>) {
        val event = incomingEvent.consume()
        if (event == null)
            return
        val info = event.makeDerivedEventInfo(EventStatus.Success)
        (event as SecondEvent).data.elements.forEach { element ->
            onProduceEvent(ThirdEvent(
                eventType = produceEvent,
                metadata = event.makeDerivedEventInfo(EventStatus.Success),
                data = element
            )
            )
        }

    }

}

