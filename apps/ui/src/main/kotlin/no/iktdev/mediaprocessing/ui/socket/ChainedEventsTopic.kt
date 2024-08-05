package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.eventi.data.derivedFromEventId
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.data.referenceId
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import no.iktdev.mediaprocessing.ui.dto.EventChain
import no.iktdev.mediaprocessing.ui.eventsManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChainedEventsTopic(
    @Autowired private val template: SimpMessagingTemplate?
) {
    @MessageMapping("/chained/all")
    fun sendAllChainedEvents() {
        val collections:  MutableMap<String, List<EventChain>> = mutableMapOf()
        eventsManager.getAllEvents().onEach { events ->
            collections[events.first().referenceId()] = events.chained()
        }
        template?.convertAndSend("/topic/chained/all",collections)
    }


    fun List<Event>.chained(): List<EventChain> {
        val eventMap = this.associateBy { it.eventId() }
        val chains = mutableMapOf<String, EventChain>()
        val children = mutableSetOf<String>()

        this.forEach { event ->
            val eventId = event.metadata.eventId
            val derivedFromEventId = event.metadata.derivedFromEventId
            val chain = chains.getOrPut(eventId) { EventChain(eventId, event.eventType.toString()) }

            if (derivedFromEventId != null && eventMap.containsKey(derivedFromEventId)) {
                val parentChain = chains.getOrPut(derivedFromEventId) {
                    EventChain(derivedFromEventId, eventMap[derivedFromEventId]!!.eventType.toString())
                }
                parentChain.elements.add(chain)
                children.add(eventId)
            }
        }

        return chains.values.filter { it.eventId !in children }
    }
}