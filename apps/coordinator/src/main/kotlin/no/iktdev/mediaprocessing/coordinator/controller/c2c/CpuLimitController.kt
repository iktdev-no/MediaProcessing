package no.iktdev.mediaprocessing.coordinator.controller.c2c

import no.iktdev.mediaprocessing.coordinator.ProcesserClient
import no.iktdev.mediaprocessing.shared.common.dto.preference.processer.CPULimit
import no.iktdev.mediaprocessing.shared.common.dto.CpuLimitSupport
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/processer/cpu-limit")
class CpuLimitController(
    private val processerClient: ProcesserClient
) {

    @GetMapping()
    fun getCpuLimit(): Mono<CPULimit> {
        val cpuLimit = processerClient.getCpuLimit()
        return cpuLimit
    }

    @PostMapping
    fun setCpuLimit(@RequestBody limit: CPULimit): Mono<ResponseEntity<String>> {
        return processerClient.setCpuLimit(limit)
    }

    @GetMapping("/support")
    fun getCpuLimitSupport(): Mono<CpuLimitSupport> {
        return processerClient.getCpuLimitSupport()
    }




}