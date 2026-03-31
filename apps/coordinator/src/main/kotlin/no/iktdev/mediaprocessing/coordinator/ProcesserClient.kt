package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.processer.CPULimit
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.util.UUID

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

    fun cancelTask(taskId: UUID): Mono<Boolean> =
        processerWebClient.get()
            .uri("/tasks/$taskId/cancel")
            .retrieve()
            .bodyToMono(Boolean::class.java)

    fun setCpuLimit(limit: CPULimit): Mono<Void> =
        processerWebClient.post()
            .uri("/processer/cpu-limit")
            .bodyValue(limit)
            .retrieve()
            .bodyToMono(Void::class.java)


    fun getCpuLimit(): Mono<CPULimit> =
        processerWebClient.get()
            .uri("/processer/cpu-limit")
            .retrieve()
            .bodyToMono(CPULimit::class.java)

    fun ping(): Mono<String> =
        processerWebClient.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(String::class.java)
}
