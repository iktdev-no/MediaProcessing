package no.iktdev.mediaprocessing.coordinator

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class ProcesserClient(
    private val webClient: WebClient
) {

    fun fetchLog(path: String): Mono<String> =
        webClient.get()
            .uri { it.path("/state/log").queryParam("path", path).build() }
            .retrieve()
            .bodyToMono(String::class.java)

    fun ping(): Mono<String> =
        webClient.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(String::class.java)
}
