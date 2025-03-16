package no.iktdev.mediaprocessing.shared.common.socket

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@EnableWebSocketMessageBroker
open class SocketImplementation: WebSocketMessageBrokerConfigurer {
    open val defaultOrigins = listOf("*://localhost:*/*", "http://localhost:3000/")
    open var additionalOrigins: List<String> = emptyList()

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        val origins = (defaultOrigins + additionalOrigins).toTypedArray()
        println("Allowing the following origins for websocket connection\n\t${origins.joinToString("\n\t")}")
        registry.addEndpoint("/ws")
            .setAllowedOrigins(*origins)
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/topic")
        registry.setApplicationDestinationPrefixes("/app")
    }
}