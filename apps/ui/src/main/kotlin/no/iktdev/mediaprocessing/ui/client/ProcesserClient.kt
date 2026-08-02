package no.iktdev.mediaprocessing.ui.client

import no.iktdev.mediaprocessing.shared.common.dto.CpuLimitSupport
import no.iktdev.mediaprocessing.shared.common.dto.ProcessCoreInfo
import no.iktdev.mediaprocessing.shared.common.dto.preference.processer.CPULimit
import no.iktdev.mediaprocessing.shared.common.dto.processer.ProcessEntry
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Component
class ProcesserClient(
    appsProperties: MediaProcessingAppsProperties,
    webClientFactory: WebClientFactory
) {
    private val client = webClientFactory.create(appsProperties.processer.address)
    private val sseClient = webClientFactory.createSse(appsProperties.processer.address)

    fun streamEvents(): Flux<ServerSentEvent<String>> {
        val typeRef = object : ParameterizedTypeReference<ServerSentEvent<String>>() {}
        return sseClient.get()
            .uri("/sse")
            .retrieve()
            .bodyToFlux(typeRef)
    }
    fun fetchLog(path: String): Mono<ResponseEntity<String>> =
        client.get()
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
        client.post()
            .uri("/tasks/$taskId/cancel")
            .retrieve()
            .bodyToMono(Boolean::class.java)

    fun setCpuLimit(limit: CPULimit): Mono<ResponseEntity<String>> =
        client.post()
            .uri("/system/cpu-limit")
            .bodyValue(limit)
            .exchangeToMono { response ->
                response.toEntity(String::class.java)
            }



    fun getCpuLimit(): Mono<CPULimit> =
        client.get()
            .uri("/system/cpu-limit")
            .retrieve()
            .bodyToMono(CPULimit::class.java)

    fun getCpuLimitSupport(): Mono<CpuLimitSupport> =
        client.get()
            .uri("/system/cpu-limit/support")
            .retrieve()
            .bodyToMono(CpuLimitSupport::class.java)

    fun getProcesses() =
        client.get()
            .uri("/system/processes")
            .retrieve()
            .bodyToFlux(ProcessEntry::class.java)
            .collectList()

    fun getProcessPinInfo(pid: Long) =
        client.get()
            .uri("/system/cpu-pin/process/$pid")
            .retrieve()
            .bodyToMono(ProcessCoreInfo::class.java)

    fun pinProcess(pid: Long, cores: List<Int>) =
        client.post()
            .uri("/system/cpu-pin/process/$pid")
            .bodyValue(cores)
            .retrieve()
            .bodyToMono(String::class.java)

    fun setGlobalPinnedCores(cores: List<Int>?) =
        client.post()
            .uri("/system/cpu-pin/global")
            .bodyValue(cores ?: emptyList<Int>())
            .retrieve()
            .bodyToMono(String::class.java)

    fun getGlobalPinnedCores() =
        client.get()
            .uri("/system/cpu-pin/global")
            .retrieve()
            .bodyToMono(List::class.java)

    fun isGlobalPinningActive() =
        client.get()
            .uri("/system/cpu-pin/global/active")
            .retrieve()
            .bodyToMono(Boolean::class.java)


    fun ping(): Mono<String> =
        client.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(String::class.java)
}