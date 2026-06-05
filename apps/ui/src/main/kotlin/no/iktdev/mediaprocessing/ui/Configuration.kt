package no.iktdev.mediaprocessing.ui

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:3000",
                "http://localhost",
                "http://localhost:80"
            )
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
    }

    override fun addViewControllers(registry: ViewControllerRegistry) {
        // Matcher alle paths uten punktum (dvs. ikke filer som .js, .css, .png)
        registry.addViewController("/{path:[^\\.]*}")
            .setViewName("forward:/index.html")
    }


    @Value("\${server.port:8080}")
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
    var scratch: String = "",
    var scratchRewrite: Rewrite? = null,

    var intermediate: String = "",
    var intermediateRewrite: Rewrite? = null,

    var outbox: String = "",
    var outboxRewrite: Rewrite? = null,

    var inbox: String = "",
    var inboxRewrite: Rewrite? = null
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

