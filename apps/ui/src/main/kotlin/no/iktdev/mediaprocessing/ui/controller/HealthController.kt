package no.iktdev.mediaprocessing.ui.controller

import no.iktdev.mediaprocessing.ui.models.contract.DiskInfo
import no.iktdev.mediaprocessing.ui.models.contract.SystemHealth
import no.iktdev.mediaprocessing.ui.models.contract.EventRate
import no.iktdev.mediaprocessing.ui.service.OperationsHealthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class HealthController(
    private val healthService: OperationsHealthService
) {

    @GetMapping
    fun getHealth(): SystemHealth {
        return healthService.getHealth()
    }

    @GetMapping("/events")
    fun getEventRate(): EventRate = healthService.getEventRate()

    @GetMapping("/storage")
    fun getDiskStatus(): List<DiskInfo> = healthService.getDiskHealth()
}