package no.iktdev.mediaprocessing.coordinator.controller.c2c

import no.iktdev.mediaprocessing.coordinator.ProcesserClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/processer/cpu-pin")
class CpuPinController(
    private val client: ProcesserClient
) {

    @GetMapping("/processes")
    fun getProcesses() =
        client.getProcesses()

    @GetMapping("/process/{pid}")
    fun getProcessPinInfo(@PathVariable pid: Long) =
        client.getProcessPinInfo(pid)

    @PostMapping("/process/{pid}")
    fun pinProcess(
        @PathVariable pid: Long,
        @RequestBody cores: List<Int>
    ) = client.pinProcess(pid, cores)

    @PostMapping("/global")
    fun setGlobalPinnedCores(@RequestBody cores: List<Int>?) =
        client.setGlobalPinnedCores(cores)

    @GetMapping("/global")
    fun getGlobalPinnedCores() =
        client.getGlobalPinnedCores()

    @GetMapping("/global/active")
    fun isGlobalPinningActive() =
        client.isGlobalPinningActive()
}
