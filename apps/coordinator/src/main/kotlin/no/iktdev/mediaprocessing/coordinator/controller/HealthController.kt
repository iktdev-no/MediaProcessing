package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.dto.CoordinatorHealth
import no.iktdev.mediaprocessing.coordinator.services.CoordinatorHealthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/health")
class HealthController(
    private val healthService: CoordinatorHealthService
) {

    @GetMapping
    fun getHealth(): CoordinatorHealth {
        return healthService.getHealth()
    }
}
