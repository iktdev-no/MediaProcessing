package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.dto.health.CoordinatorHealth
import no.iktdev.mediaprocessing.coordinator.dto.rate.EventRate
import no.iktdev.mediaprocessing.coordinator.services.CoordinatorHealthService
import no.iktdev.mediaprocessing.coordinator.util.DiskInfo
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

    @GetMapping("/health/events")
    fun getEventRate(): EventRate = healthService.getEventRate()

    @GetMapping("/health/storage")
    fun getDiskStatus(): List<DiskInfo> = healthService.getDiskHealth()
}
