package no.iktdev.mediaprocessing.processer.controller

import no.iktdev.mediaprocessing.processer.services.ProcessService
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.processer.CPULimit
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
    fun getCpuLimit(): ResponseEntity<CPULimit> =
        ResponseEntity.ok(processService.getGlobalCpuLimit())

    @PostMapping("/cpu-limit")
    fun setCpuLimit(@RequestBody limit: CPULimit): ResponseEntity<String> {
        if (limit.limit !in 1..100) {
            return ResponseEntity.badRequest().body("percent must be between 1 and 100")
        }

        processService.updateCpuLimit(limit)
        return ResponseEntity.ok("CPU limit updated successfully")
    }
}
