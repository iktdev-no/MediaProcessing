package no.iktdev.mediaprocessing.coordinator

import no.iktdev.mediaprocessing.shared.common.dto.preference.processer.CPULimit
import no.iktdev.mediaprocessing.shared.common.dto.CpuLimitSupport
import no.iktdev.mediaprocessing.shared.common.dto.ProcessCoreInfo
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.ProcessEntry
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.jvm.java

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

    fun setCpuLimit(limit: CPULimit): Mono<ResponseEntity<String>> =
        processerWebClient.post()
            .uri("/system/cpu-limit")
            .bodyValue(limit)
            .exchangeToMono { response ->
                response.toEntity(String::class.java)
            }



    fun getCpuLimit(): Mono<CPULimit> =
        processerWebClient.get()
            .uri("/system/cpu-limit")
            .retrieve()
            .bodyToMono(CPULimit::class.java)

    fun getCpuLimitSupport(): Mono<CpuLimitSupport> =
        processerWebClient.get()
            .uri("/system/cpu-limit/support")
            .retrieve()
            .bodyToMono(CpuLimitSupport::class.java)

    fun getProcesses() =
        processerWebClient.get()
            .uri("/system/processes")
            .retrieve()
            .bodyToFlux(ProcessEntry::class.java)
            .collectList()

    fun getProcessPinInfo(pid: Long) =
        processerWebClient.get()
            .uri("/system/cpu-pin/process/$pid")
            .retrieve()
            .bodyToMono(ProcessCoreInfo::class.java)

    fun pinProcess(pid: Long, cores: List<Int>) =
        processerWebClient.post()
            .uri("/system/cpu-pin/process/$pid")
            .bodyValue(cores)
            .retrieve()
            .bodyToMono(String::class.java)

    fun setGlobalPinnedCores(cores: List<Int>?) =
        processerWebClient.post()
            .uri("/system/cpu-pin/global")
            .bodyValue(cores ?: emptyList<Int>())
            .retrieve()
            .bodyToMono(String::class.java)

    fun getGlobalPinnedCores() =
        processerWebClient.get()
            .uri("/system/cpu-pin/global")
            .retrieve()
            .bodyToMono(List::class.java)

    fun isGlobalPinningActive() =
        processerWebClient.get()
            .uri("/system/cpu-pin/global/active")
            .retrieve()
            .bodyToMono(Boolean::class.java)


    fun ping(): Mono<String> =
        processerWebClient.get()
            .uri("/actuator/health")
            .retrieve()
            .bodyToMono(String::class.java)
}
