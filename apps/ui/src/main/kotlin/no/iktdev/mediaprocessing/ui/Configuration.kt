package no.iktdev.mediaprocessing.ui

import no.iktdev.mediaprocessing.shared.common.Defaults
import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaImplementation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebConfig: WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins("localhost", "*://localhost:3000", "localhost:80")
            .allowCredentials(false)
    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController::class.java))
     //   configurer.addPathPrefix("/ws", HandlerTypePredicate.forAnnotation(Controller::class.java))
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(0)
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
    fun coordinatorTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        return restTemplate
    }
}


@Configuration
class SocketImplemented: SocketImplementation() {
}

@Configuration
class DefaultConfiguration: Defaults()

@Configuration
@Import(CoordinatorProducer::class, DefaultMessageListener::class)
class KafkaLocalInit: KafkaImplementation() {
}