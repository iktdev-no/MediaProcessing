package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.eventi.data.*
import no.iktdev.eventi.database.toEpochSeconds
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.ui.dto.EventChain
import no.iktdev.mediaprocessing.ui.dto.EventHolder
import no.iktdev.mediaprocessing.ui.eventsManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.io.File

@Controller
class ChainedEventsTopic(
    @Autowired private val template: SimpMessagingTemplate?,
) {
    @MessageMapping("/chained/all")
    fun sendAllChainedEvents() {
        val holders: MutableList<EventHolder> = mutableListOf()
        eventsManager.getAllEvents().onEach { events ->
            holders.add(EventHolder(
                referenceId = events.first().referenceId(),
                fileName = events.findFirstOf(Events.ProcessStarted)?.dataAs<StartEventData>()?.file?.let { File(it).name },
                events = events.chained(),
                created = events.chained().firstOrNull()?.created ?: 0
            ))
        }
        template?.convertAndSend("/topic/chained/all", holders)
    }


    fun List<Event>.chained(): List<EventChain> {
        val eventMap = this.associateBy { it.eventId() }
        val chains = mutableMapOf<String, EventChain>()
        val children = mutableSetOf<String>()

        this.forEach { event ->
            val eventId = event.metadata.eventId
            val derivedFromEventId = event.metadata.derivedFromEventId
            val created = event.metadata.created.toEpochSeconds() * 1000L
            val chain = chains.getOrPut(eventId) {
                EventChain(eventId, event.eventType.toString(), created, success = event.isSuccessful(), skipped = event.isSkipped(), failure = event.isFailed())
            }

            if (derivedFromEventId != null && eventMap.containsKey(derivedFromEventId)) {
                val parentChain = chains.getOrPut(derivedFromEventId) {
                    EventChain(derivedFromEventId, eventMap[derivedFromEventId]!!.eventType.toString(), created,  success = event.isSuccessful(), skipped = event.isSkipped(), failure = event.isFailed())
                }
                parentChain.events.add(chain)
                children.add(eventId)
            }
        }

        chains.values.forEach { chain -> chain.events.sortBy { it.created }}

        return chains.values.filter { it.eventId !in children }
            .sortedBy { it.created }
    }
}