package no.iktdev.mediaprocessing.coordinator.controller.c2c

import no.iktdev.mediaprocessing.coordinator.ProcesserClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/processer/tasks")
class TasksController(
    private val processerClient: ProcesserClient
) {

    @GetMapping("/{taskId}/cancel")
    fun cancel(@PathVariable taskId: UUID): Mono<Boolean> {
        return processerClient.cancelTask(taskId)
    }

}
