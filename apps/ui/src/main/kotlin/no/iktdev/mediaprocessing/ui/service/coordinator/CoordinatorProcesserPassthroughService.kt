package no.iktdev.mediaprocessing.ui.service.coordinator

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class CoordinatorProcesserPassthroughService(
    private val coordinatorWebClient: WebClient,

) {
    fun getLog(path: String): Mono<String> =
        coordinatorWebClient.get()
            .uri { builder ->
                builder.path("/processer/log")
                    .queryParam("path", path)
                    .build()
            }
            .retrieve()
            .bodyToMono(String::class.java)
}