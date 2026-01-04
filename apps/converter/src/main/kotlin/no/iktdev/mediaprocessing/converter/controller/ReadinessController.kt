package no.iktdev.mediaprocessing.converter.controller

import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.boot.actuate.health.Status
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/system")
class ReadinessController(
    private val healthEndpoint: HealthEndpoint
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
}
