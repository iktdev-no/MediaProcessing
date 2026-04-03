package no.iktdev.mediaprocessing.processer.controller

import no.iktdev.mediaprocessing.processer.services.ProcessService
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CpuLimitSupport
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.ProcessCoreInfo
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.processer.CPULimit
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.Status
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/system/cpu-pin")
class CpuPinController(
    private val processService: ProcessService
) {

    // ---------------------------
    // GLOBAL PINNING
    // ---------------------------

    @PostMapping("/global")
    fun setGlobalPinnedCores(@RequestBody cores: List<Int>?): ResponseEntity<String> {
        processService.setGlobalPinnedCores(cores)
        return ResponseEntity.ok("Global pinned cores updated")
    }

    @GetMapping("/global")
    fun getGlobalPinnedCores(): ResponseEntity<List<Int>?> =
        ResponseEntity.ok(processService.getGlobalPinnedCores())

    @GetMapping("/global/active")
    fun isGlobalPinningActive(): ResponseEntity<Boolean> =
        ResponseEntity.ok(processService.isGlobalPinningActive())


    // ---------------------------
    // PER-PID PINNING
    // ---------------------------

    @PostMapping("/process/{pid}")
    fun pinProcess(
        @PathVariable pid: Long,
        @RequestBody cores: List<Int>
    ): ResponseEntity<String> {
        processService.pinProcessToCores(pid, cores)
        return ResponseEntity.ok("Process pinned to cores")
    }

    @GetMapping("/process/{pid}")
    fun getProcessPinInfo(@PathVariable pid: Long): ResponseEntity<ProcessCoreInfo> =
        ResponseEntity.ok(
            ProcessCoreInfo(
                assigned = processService.getAssignedCores(pid),
                manual = processService.getManuallyPinnedCores(pid),
                effective = processService.getEffectiveCores(pid),
                percent = processService.getPercentLimit(pid)
            )
        )
}
