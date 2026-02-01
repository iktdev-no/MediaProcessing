package no.iktdev.mediaprocessing.ui

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost",
                "http://localhost:80"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }

    @Value("\${APP_DEPLOYMENT_PORT:8080}")
    private val deploymentPort = 8080

    @Bean
    fun webServerFactoryCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
        return WebServerFactoryCustomizer { factory ->
            factory.port = deploymentPort
        }
    }
}


@ConfigurationProperties(prefix = "media")
data class MediaConfig(
    var cache: String = "",
    var cacheRewrite: Rewrite? = null,

    var outgoing: String = "",
    var outgoingRewrite: Rewrite? = null,

    var incoming: String = "",
    var incomingRewrite: Rewrite? = null
) {
    data class Rewrite(
        var to: String = ""
    )
}


@ConfigurationProperties(prefix = "mediaprocessing.apps")
data class AppsConfig(
    val coordinator: AppConfig,
    val processer: AppConfig,
    val converter: AppConfig,
    val metadata: AppConfig,
    val watcher: AppConfig
)

data class AppConfig(
    val address: String,
    val health: String
)


@Configuration
class HttpConfig {
    @Bean
    fun restTemplate() = RestTemplate()
}

@Configuration
class WebClientConfig(
    private val appsConfig: AppsConfig
) {

    @Bean
    fun coordinatorWebClient(builder: WebClient.Builder): WebClient =
        builder
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .baseUrl(appsConfig.coordinator.address).build()

    @Bean
    fun webClient(): WebClient.Builder =
        WebClient
            .builder()
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }


}
