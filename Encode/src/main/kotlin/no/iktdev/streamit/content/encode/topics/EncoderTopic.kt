package no.iktdev.streamit.content.encode.topics

import no.iktdev.exfl.observable.ObservableMap
import no.iktdev.streamit.content.common.dto.WorkOrderItem
import no.iktdev.streamit.content.encode.encoderItems
import no.iktdev.streamit.content.encode.extractItems
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class EncoderTopic(
    @Autowired val template: SimpMessagingTemplate?,
) {

    init {
        encoderItems.addListener(object : ObservableMap.Listener<String, WorkOrderItem> {
            override fun onMapUpdated(map: Map<String, WorkOrderItem>) {
                super.onMapUpdated(map)
                pushEncoderQueue()
            }

            override fun onPut(key: String, value: WorkOrderItem) {
                super.onPut(key, value)
                pushEncoderWorkOrder(value)
            }
        })
        extractItems.addListener(object : ObservableMap.Listener<String, WorkOrderItem> {
            override fun onMapUpdated(map: Map<String, WorkOrderItem>) {
                super.onMapUpdated(map)
                pushExtractorQueue()
            }

            override fun onPut(key: String, value: WorkOrderItem) {
                super.onPut(key, value)
                pushExtractorWorkOrder(value)
            }
        })
    }

    fun pushEncoderWorkOrder(item: WorkOrderItem) {
        template?.convertAndSend("/topic/encoder/workorder", item)
    }

    fun pushExtractorWorkOrder(item: WorkOrderItem) {
        template?.convertAndSend("/topic/extractor/workorder", item)
    }

    @MessageMapping("/encoder/queue")
    fun pushEncoderQueue() {
        template?.convertAndSend("/topic/encoder/queue", encoderItems.values)
    }

    @MessageMapping("/extractor/queue")
    fun pushExtractorQueue() {
        template?.convertAndSend("/topic/extractor/queue", extractItems.values)

    }





}