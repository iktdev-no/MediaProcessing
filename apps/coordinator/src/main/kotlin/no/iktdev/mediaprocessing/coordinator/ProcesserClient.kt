package no.iktdev.mediaprocessing.coordinator

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class ProcesserClient(
    private val processerWebClient: WebClient
) {

    fun fetchLog(path: String): Mono<ResponseEntity<String>> =
        processerWebClient.get()
            .uri { it.path("/state/log").queryParam("path", path).build() }
            .exchangeToMono { response ->
                response.bodyToMono(String::class.java)
                    .map { body ->
                        ResponseEntity
                            .status(response.statusCode())
                            .headers(response.headers().asHttpHeaders())
                            .body(body)
                    }
            }


    fun ping(): Mono<String> =
        processerWebClient.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(String::class.java)
}
