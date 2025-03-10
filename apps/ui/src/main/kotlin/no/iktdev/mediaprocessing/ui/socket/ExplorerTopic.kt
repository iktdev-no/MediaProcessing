package no.iktdev.mediaprocessing.ui.socket

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.contract.dto.EventRequest
import no.iktdev.mediaprocessing.ui.UIEnv
import no.iktdev.mediaprocessing.ui.explorer.ExplorerCore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.client.RestTemplate

val log = KotlinLogging.logger {}
@Controller
class ExplorerTopic(
    @Autowired private val template: SimpMessagingTemplate?,
    @Autowired private val coordinatorTemplate: RestTemplate,
    val explorer: ExplorerCore = ExplorerCore()
) {

    @MessageMapping("/explorer/home")
    fun goHome() {
        explorer.getHomeCursor()?.let {
            template?.convertAndSend("/topic/explorer/go", it)
        }
    }

    @MessageMapping("/explorer/navigate")
    fun navigateTo(@Payload path: String) {
        val cursor = explorer.getCursor(path)
        cursor?.let {
            template?.convertAndSend("/topic/explorer/go", it)
        }
    }

    @MessageMapping("/request/encode")
    fun requestEncode(@Payload data: EventRequest) {
        val req = coordinatorTemplate.postForEntity("/request/encode", data, String::class.java)
        log.info { req }
    }

    @MessageMapping("/request/extract")
    fun requestExtract(@Payload data: EventRequest) {
        val req = coordinatorTemplate.postForEntity("/request/extract", data, String::class.java)
        log.info { req }
    }

    @MessageMapping("/request/convert")
    fun requestConvert(@Payload data: EventRequest) {
        val req = coordinatorTemplate.postForEntity("/request/convert", data, String::class.java)
        log.info { req }
    }

    @MessageMapping("/request/all")
    fun requestAllAvailableActions(@Payload data: EventRequest) {
        log.info { "Sending data to coordinator: ${Gson().toJson(data)}" }
        val req = coordinatorTemplate.postForEntity("/request/all", data, String::class.java)
        log.info { req }
    }

}