package no.iktdev.streamit.content.ui.socket

import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.io.File

@Controller
class RequestTopic(
    @Autowired private val template: SimpMessagingTemplate?
) {
    val messageProducer = DefaultProducer(CommonConfig.kafkaTopic)

    @MessageMapping("/request/start")
    fun requestStartOn(@Payload fullName: String) {
        val file = File(fullName)
        if (file.exists()) {
            try {
                val message = Message(
                    status = Status(Status.SUCCESS),
                    data = fullName
                )
                messageProducer.sendMessage(KafkaEvents.REQUEST_FILE_READ.event, message)
                template?.convertAndSend("/response/request", RequestResponse(true, fullName))

            } catch (e: Exception) {
                template?.convertAndSend("/response/request", RequestResponse(false, fullName))

            }
        } else {
            template?.convertAndSend("/response/request", RequestResponse(false, fullName))
        }

    }
}

data class RequestResponse(
    val success: Boolean,
    val file: String
)