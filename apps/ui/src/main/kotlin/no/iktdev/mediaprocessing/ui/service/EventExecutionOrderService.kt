package no.iktdev.mediaprocessing.ui.service

import no.iktdev.eventi.data.derivedFromEventId
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.data.referenceId
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import no.iktdev.mediaprocessing.shared.common.database.cal.EventsManager
import no.iktdev.mediaprocessing.ui.dto.EventChain
import no.iktdev.mediaprocessing.ui.eventsManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class EventExecutionOrderService(
    @Autowired eventsManager: EventsManager
) {

    val collections:  MutableMap<String, List<EventChain>> = mutableMapOf()

    @Scheduled(fixedDelay = 5_000)
    fun pullAvailableEvents() {
        eventsManager.getAllEvents().onEach { events ->
            collections[events.first().referenceId()] = events.chained()
        }
    }


    fun List<Event>.chained(): List<EventChain> {
        val eventMap = this.associateBy { it.eventId() }
        val chains = mutableMapOf<String, EventChain>()

        this.forEach { event ->
            val chain = EventChain(eventId = event.eventId(), eventName = event.eventType.name)
            chains[event.eventId()] = chain

            if (event.derivedFromEventId() != null && eventMap.containsKey(event.derivedFromEventId())) {
                val parentChain = chains[event.derivedFromEventId()]
                parentChain?.elements?.add(chain)
            }
        }
        return chains.values.filter { it.elements.isNotEmpty() }.toList()
    }



}