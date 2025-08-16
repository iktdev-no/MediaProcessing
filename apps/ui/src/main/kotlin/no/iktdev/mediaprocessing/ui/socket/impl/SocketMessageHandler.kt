package no.iktdev.mediaprocessing.ui.socket.impl

import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import java.lang.reflect.Type

open class SocketMessageHandler: StompSessionHandlerAdapter() {
    override fun getPayloadType(headers: StompHeaders): Type {
        return ByteArray::class.java
    }

    override fun handleFrame(headers: StompHeaders, payload: Any?) {
        super.handleFrame(headers, payload)
        if (payload is ByteArray) {
            onMessage(String(payload))
        } else if (payload is String) {
            onMessage(payload)
        }
    }

    open fun onMessage(socketMessage: String) {
    }
}