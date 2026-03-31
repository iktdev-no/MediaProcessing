package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.processer.CPULimit
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorProcesserPassthroughService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/processer")
class CoordinatorPassthroughController(
    private val coordinator: CoordinatorProcesserPassthroughService,
) {
    @GetMapping("/logs")
    fun getLog(@RequestParam path: String): Mono<String> {
        return coordinator.getLog(path)
    }

    @GetMapping("/tasks/{taskId}/cancel")
    fun cancel(@PathVariable taskId: UUID): Mono<Boolean> {
        return coordinator.cancelTask(taskId)
    }

    @GetMapping("/cpu-limit")
    fun getCpuLimit(): Mono<CPULimit> {
        return coordinator.getCpuLimit()
    }

    @PostMapping("/cpu-limit")
    fun setCpuLimit(@PathVariable limit: CPULimit): Mono<Void> {
        return coordinator.setCpuLimit(limit)
    }
}
