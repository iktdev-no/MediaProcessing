package no.iktdev.mediaprocessing.ui

import no.iktdev.mediaprocessing.shared.common.Defaults
import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.*
import org.springframework.web.servlet.resource.PathResourceResolver
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.util.DefaultUriBuilderFactory
import java.util.concurrent.ConcurrentHashMap


@Configuration
class WebConfig: WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("localhost", "*://localhost:3000", "localhost:80")
            .allowCredentials(true)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {

        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object: PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    // Show index.html if no resource was found
                    return if (!location.createRelative(resourcePath).exists() && !location.createRelative(resourcePath).isReadable) {
                        location.createRelative("index.html");
                    } else {
                        location.createRelative(resourcePath);
                    }
                }
            })

    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController::class.java))
    }


    @Value("\${APP_DEPLOYMENT_PORT:8080}")
    private val deploymentPort = 8080

    @Bean
    fun webServerFactoryCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory>? {
        return WebServerFactoryCustomizer { factory: TomcatServletWebServerFactory ->
            factory.port = deploymentPort
        }
    }
}

@Configuration
class ApiCommunicationConfig {

    @Bean
    fun coordinatorTemplate(builder: RestTemplateBuilder): RestTemplate {
        try {
            val url = UIEnv.coordinatorUrl
            log.info { "CoordinatorUrl: $url" }
            require(url.isNotBlank()) { "UIEnv.coordinatorUrl er ikke satt!" }
            return builder
                .uriTemplateHandler(DefaultUriBuilderFactory(url)) // Bruker den returnerte instansen
                .build()

        } catch (e: Exception) {
            throw IllegalStateException("Feil ved opprettelse av coordinatorTemplate: ${e.message}", e)
        }
    }

}


@Configuration
class SocketImplemented: SocketImplementation() {
    override var additionalOrigins: List<String> = UIEnv.wsAllowedOrigins.split(",")
}

@Service
class WebSocketMonitoringService() {
    private val clients = ConcurrentHashMap.newKeySet<WebSocketSession>()
    fun anyListening() = clients.isNotEmpty()

    fun addClient(session: WebSocketSession) {
        clients.add(session)
    }
    fun removeClient(session: WebSocketSession) {
        clients.remove(session)
    }
}

@Component
class WebSocketHandler(private val webSocketPollingService: WebSocketMonitoringService) : TextWebSocketHandler() {

    // Kalles når en WebSocket-klient kobler til
    override fun afterConnectionEstablished(session: WebSocketSession) {
        webSocketPollingService.addClient(session) // Legg til klienten i service
    }

    // Kalles når en WebSocket-klient kobler fra
    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        webSocketPollingService.removeClient(session) // Fjern klienten fra service
    }

    // Håndterer meldinger fra WebSocket-klientene hvis nødvendig
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // Håndter meldinger fra klienten
    }
}

@Configuration
class DefaultConfiguration: Defaults()
