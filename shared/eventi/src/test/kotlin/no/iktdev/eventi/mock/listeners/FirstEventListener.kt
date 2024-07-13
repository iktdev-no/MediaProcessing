package no.iktdev.eventi.mock.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.mock.MockDataEventListener
import no.iktdev.eventi.mock.MockEventCoordinator
import no.iktdev.eventi.mock.data.FirstEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class FirstEventListener() : MockDataEventListener() {
    @Autowired
    override var coordinator: MockEventCoordinator? = null

    private val log = KotlinLogging.logger {}

    init {
        log.info { "Created Service: ${this::class.java.simpleName}" }
    }

    override val produceEvent = this::class.java.simpleName
    override val listensForEvents = listOf("Init")


    override fun onProduceEvent(event: EventImpl) {
        super.onProduceEvent(event)
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<EventImpl>, events: List<EventImpl>) {
        val info = incomingEvent.consume()!!.makeDerivedEventInfo(EventStatus.Success)
        onProduceEvent(FirstEvent(
            eventType = produceEvent,
            metadata = info,
            data = "Potet"
        ))
    }


}

