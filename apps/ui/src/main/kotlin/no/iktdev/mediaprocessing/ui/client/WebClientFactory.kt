package no.iktdev.mediaprocessing.ui.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

import io.netty.resolver.DefaultAddressResolverGroup
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.http.client.HttpClient

@Component
class WebClientFactory(
    private val webClientBuilder: WebClient.Builder
) {
    // Standard klient for vanlig REST (CRUD, post, get)
    fun create(baseUrl: String): WebClient {
        return webClientBuilder
            .clone()
            .baseUrl(baseUrl)
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .build()
    }

    // Spesialklient for SSE-strømmer (hindrer tidsavbrudd og holder tilkoblingen åpen)
    fun createSse(baseUrl: String): WebClient {
        val httpClient = HttpClient.create()
            .compress(true)
            .keepAlive(false)
            .resolver(DefaultAddressResolverGroup.INSTANCE)
        // Valgfritt: skru av read timeout for SSE hvis du vil unngå at den timer ut
        // .responseTimeout(java.time.Duration.ofDays(1))

        return webClientBuilder
            .clone()
            .baseUrl(baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .build()
    }
}