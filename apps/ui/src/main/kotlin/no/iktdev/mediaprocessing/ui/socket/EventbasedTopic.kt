package no.iktdev.mediaprocessing.ui.socket

import mu.KotlinLogging
import no.iktdev.exfl.observable.ObservableList
import no.iktdev.exfl.observable.ObservableMap
import no.iktdev.exfl.observable.observableListOf
import no.iktdev.exfl.observable.observableMapOf
import no.iktdev.mediaprocessing.ui.dto.EventDataObject
import no.iktdev.mediaprocessing.ui.dto.EventSummary
import no.iktdev.mediaprocessing.ui.dto.SimpleEventDataObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class EventbasedTopic(
    @Autowired private val template: SimpMessagingTemplate?
) {
    private val log = KotlinLogging.logger {}
    val summaryList: ObservableList<EventSummary> = observableListOf()
    val memSimpleConvertedEventsMap: ObservableMap<String, SimpleEventDataObject> = observableMapOf()
    val memActiveEventMap: ObservableMap<String, EventDataObject> = observableMapOf()

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
        summaryList.addListener(object: ObservableList.Listener<EventSummary> {
            override fun onListChanged(items: List<EventSummary>) {
                super.onListChanged(items)
                template?.convertAndSend("/topic/summary", items)
            }
        })
    }

    @MessageMapping("/items")
    fun sendItems() {
        template?.convertAndSend("/topic/event/items", memActiveEventMap.values.reversed())
        template?.convertAndSend("/topic/event/flat", memSimpleConvertedEventsMap.values.reversed())
    }

}