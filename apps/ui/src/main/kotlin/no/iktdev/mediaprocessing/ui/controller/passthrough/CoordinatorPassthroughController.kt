package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorProcesserPassthroughService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/processer/logs")
class CoordinatorPassthroughController(
    private val coordinator: CoordinatorProcesserPassthroughService,
) {
    @GetMapping
    fun getLog(@RequestParam path: String): Mono<String> {
        return coordinator.getLog(path)
    }
}
