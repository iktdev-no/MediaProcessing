package no.iktdev.mediaprocessing.ui.socket

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

    @MessageMapping("/request/convert")
    fun requestConvert(@Payload data: EventRequest) {
        val req = coordinatorTemplate.postForEntity(UIEnv.coordinatorUrl, data, String.javaClass)
        log.info { req }
    }

    @MessageMapping("/request/all")
    fun requestAllAvailableActions() {

    }

}