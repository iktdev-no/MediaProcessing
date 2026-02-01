package no.iktdev.mediaprocessing.processer.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class CoordinatorClientConfig(
    private val processerProperties: ProcesserProperties,
) {

    @Bean
    fun coordinatorWebClient(builder: WebClient.Builder): WebClient {
        val baseUrl = processerProperties.coordinatorUrl
            ?: error("COORDINATOR_URL must be set")

        return builder
            .baseUrl(baseUrl)
            .build()
    }
}

