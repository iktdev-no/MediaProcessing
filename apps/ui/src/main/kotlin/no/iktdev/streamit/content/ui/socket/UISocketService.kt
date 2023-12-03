package no.iktdev.streamit.content.ui.socket

import mu.KotlinLogging
import no.iktdev.exfl.observable.ObservableMap
import no.iktdev.streamit.content.ui.dto.EventDataObject
import no.iktdev.streamit.content.ui.dto.SimpleEventDataObject
import no.iktdev.streamit.content.ui.memActiveEventMap
import no.iktdev.streamit.content.ui.memSimpleConvertedEventsMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class UISocketService(
    @Autowired private val template: SimpMessagingTemplate?
) {
    private val log = KotlinLogging.logger {}

    init {
        memActiveEventMap.addListener(object : ObservableMap.Listener<String, EventDataObject> {
            override fun onMapUpdated(map: Map<String, EventDataObject>) {
                super.onMapUpdated(map)
                log.info { "Sending data to WS" }
                template?.convertAndSend("/topic/event/items", map.values.reversed())
                if (template == null) {
                    log.error { "Template is null!" }
                }
            }
        })
        memSimpleConvertedEventsMap.addListener(object : ObservableMap.Listener<String, SimpleEventDataObject> {
            override fun onMapUpdated(map: Map<String, SimpleEventDataObject>) {
                super.onMapUpdated(map)
                log.info { "Sending data to WS" }
                template?.convertAndSend("/topic/event/flat", map.values.reversed())
                if (template == null) {
                    log.error { "Template is null!" }
                }
            }
        })
    }

    @MessageMapping("/items")
    fun sendItems() {
        template?.convertAndSend("/topic/event/items", memActiveEventMap.values.reversed())
        template?.convertAndSend("/topic/event/flat", memSimpleConvertedEventsMap.values.reversed())
    }

}