package no.iktdev.streamit.content.ui.socket

import no.iktdev.streamit.content.ui.explorer.ExplorerCore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ExplorerTopic(
    @Autowired private val template: SimpMessagingTemplate?,
    val explorer: ExplorerCore = ExplorerCore()
): TopicSupport() {

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


}