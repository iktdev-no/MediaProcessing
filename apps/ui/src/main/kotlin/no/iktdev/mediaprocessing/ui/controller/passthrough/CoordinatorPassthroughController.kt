package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.shared.common.dto.EventQuery
import no.iktdev.mediaprocessing.ui.dto.Paginated
import no.iktdev.mediaprocessing.ui.dto.UiEvent
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorEventService
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorProcesserPassthroughService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.*

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
