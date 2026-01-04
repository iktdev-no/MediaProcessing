package no.iktdev.mediaprocessing.processer

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class CoordinatorClientConfig {

    @Bean
    fun coordinatorWebClient(builder: WebClient.Builder): WebClient {
        val baseUrl = ProcesserEnv.coordinatorUrl
            ?: error("COORDINATOR_URL must be set")

        return builder
            .baseUrl(baseUrl)
            .build()
    }
}
