package no.iktdev.mediaprocessing.coordinator.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@ConfigurationProperties(prefix = "processer")
data class ProcesserClientProperties(
    val baseUrl: String
)

