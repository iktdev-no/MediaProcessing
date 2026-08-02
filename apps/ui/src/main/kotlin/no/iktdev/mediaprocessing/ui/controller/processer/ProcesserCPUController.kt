package no.iktdev.mediaprocessing.ui.controller.processer

import no.iktdev.mediaprocessing.shared.common.dto.preference.processer.CPULimit
import no.iktdev.mediaprocessing.ui.client.ProcesserClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/processer")
class ProcesserCPUController(
    private val processerClient: ProcesserClient
) {
    @GetMapping("/cpu-limit")
    fun getCpuLimit() = processerClient.getCpuLimit()
    @PostMapping("/cpu-limit")
    fun setCpuLimit(@RequestBody limit: CPULimit) = processerClient.setCpuLimit(limit)
    @GetMapping("/cpu-limit/supported")
    fun getCpuLimitSupported() = processerClient.getCpuLimitSupport()


    @GetMapping("/cpu-pin/global/active")
    fun isGlobalPinningActive() = processerClient.isGlobalPinningActive()
    @GetMapping("/cpu-pin/global/pinned-cores")
    fun getGlobalPinnedCores() = processerClient.getGlobalPinnedCores()
    @PostMapping("/cpu-pin/global/pinned-cores")
    fun setGlobalPinnedCores(@RequestBody cores: List<Int>?) = processerClient.setGlobalPinnedCores(cores)

    @GetMapping("/cpu-pin/process/pinned-cores/{pid}")
    fun getProcessPinnedCores(@PathVariable pid: Long) = processerClient.getProcessPinInfo(pid)
    @PostMapping("/cpu-pin/process/pinned-cores/{pid}")
    fun setProcessPinnedCores(@PathVariable pid: Long, @RequestBody cores: List<Int>) = processerClient.pinProcess(pid, cores)

    @GetMapping("/processes")
    fun getProcesses() = processerClient.getProcesses()



}