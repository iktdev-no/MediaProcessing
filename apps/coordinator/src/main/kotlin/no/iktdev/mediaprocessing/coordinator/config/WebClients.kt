package no.iktdev.mediaprocessing.coordinator.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClients(
    private val processerClientProperties: ProcesserClientProperties

) {
    @Bean
    fun webClient(): WebClient.Builder =
        WebClient
            .builder()
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
    @Bean
    fun processerWebClient(builder: WebClient.Builder): WebClient {
        return builder.baseUrl(processerClientProperties.baseUrl).build()
    }
}
