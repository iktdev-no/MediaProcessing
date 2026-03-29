package no.iktdev.mediaprocessing.processer.controller

import no.iktdev.mediaprocessing.processer.services.ProcessService
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.Status
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/system")
class CommandController(
    private val healthEndpoint: HealthEndpoint,
    private val processService: ProcessService
) {

    @GetMapping("/ready")
    fun ready(): ResponseEntity<String> {
        val health = healthEndpoint.health()

        return if (health.status == Status.UP) {
            ResponseEntity.ok("READY")
        } else {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body("NOT_READY: ${health.status}")
        }
    }

    // ---------------------------------------------------------
    // CPU LIMIT ENDPOINTS
    // ---------------------------------------------------------

    @GetMapping("/cpu-limit")
    fun getCpuLimit(): ResponseEntity<Int> =
        ResponseEntity.ok(processService.getGlobalCpuLimitPercent())

    @PostMapping("/cpu-limit/{percent}")
    fun setCpuLimit(@PathVariable percent: Int): ResponseEntity<String> {
        if (percent !in 1..100) {
            return ResponseEntity.badRequest().body("percent must be between 1 and 100")
        }

        processService.setGlobalCpuLimit(percent)
        return ResponseEntity.ok("CPU limit updated to $percent%")
    }
}
