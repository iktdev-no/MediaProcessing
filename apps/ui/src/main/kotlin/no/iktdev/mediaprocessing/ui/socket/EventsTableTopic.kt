package no.iktdev.mediaprocessing.ui.socket

import no.iktdev.mediaprocessing.ui.service.PersistentEventsTableService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class EventsTableTopic(
    @Autowired private val template: SimpMessagingTemplate?,
    @Autowired private val persistentEventsTableService: PersistentEventsTableService
): TopicSupport() {

    @MessageMapping("/persistent/events")
    fun readbackEvents() {
        template?.convertAndSend("/topic/persistent/events", persistentEventsTableService.cachedEvents)
    }

}