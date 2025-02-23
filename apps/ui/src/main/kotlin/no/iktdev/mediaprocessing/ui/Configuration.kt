package no.iktdev.mediaprocessing.ui

import no.iktdev.mediaprocessing.shared.common.Defaults
import no.iktdev.mediaprocessing.shared.common.socket.SocketImplementation
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.*
import org.springframework.web.util.DefaultUriBuilderFactory
import org.springframework.web.util.UriTemplateHandler


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
            .setCachePeriod(0)
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Endrer på denne linjen for å være mer presis
        registry.addViewController("/")
            .setViewName("forward:/index.html")

        // Denne fanger andre ruter som ikke starter med `/api`
        registry.addViewController("/**/{spring:[^api].*}")
            .setViewName("forward:/index.html")
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
        return try {
            val url = UIEnv.coordinatorUrl
            log.info { "CoordinatorUrl: $url" }
            require(url.isNotBlank()) { "UIEnv.coordinatorUrl er ikke satt!" }
            builder.uriTemplateHandler(DefaultUriBuilderFactory(url))
            builder.build()
        } catch (e: Exception) {
            throw IllegalStateException("Feil ved opprettelse av coordinatorTemplate: ${e.message}", e)
        }
    }

}


@Configuration
class SocketImplemented: SocketImplementation() {
}

@Configuration
class DefaultConfiguration: Defaults()
