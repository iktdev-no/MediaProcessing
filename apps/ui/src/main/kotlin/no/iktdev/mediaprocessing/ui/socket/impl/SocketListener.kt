package no.iktdev.mediaprocessing.ui.socket.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class SocketListener(
    @Autowired protected val template: SimpMessagingTemplate?,
) {
}