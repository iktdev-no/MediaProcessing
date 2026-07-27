package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorHealth
import no.iktdev.mediaprocessing.shared.common.dto.DiskInfo
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.EventRate
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorHealthService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/health")
class HealthController(
    private val coordinator: CoordinatorHealthService,
) {
    @GetMapping()
    fun getHealth(): Mono<CoordinatorHealth> {
        return coordinator.getHealth()
    }

    @GetMapping("/events")
    fun getHealthEventRate(): Mono<EventRate> {
        return coordinator.getEventRate()
    }

    @GetMapping("/storage")
    fun getHealthStorage(): Mono<List<DiskInfo>> {
        return coordinator.getHealthStorage()
    }
}
