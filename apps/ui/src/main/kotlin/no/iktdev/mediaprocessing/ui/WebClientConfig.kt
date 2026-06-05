package no.iktdev.mediaprocessing.ui

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

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

    @Bean
    fun sseWebClient(): WebClient {
        val httpClient = HttpClient.create()
            .compress(true)
            .keepAlive(false)
            .resolver(DefaultAddressResolverGroup.INSTANCE)

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }
}