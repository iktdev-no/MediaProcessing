package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.shared.common.dto.ResetTaskResponse
import no.iktdev.mediaprocessing.shared.common.dto.TaskQuery
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.Progress
import no.iktdev.mediaprocessing.ui.dto.UiTask
import no.iktdev.mediaprocessing.ui.dto.Paginated
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorTaskService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/api/tasks")
class TaskController(
    private val coordinator: CoordinatorTaskService,
) {

    @GetMapping("/names")
    fun getExpectedTaskNames(): Mono<List<String>> {
        return coordinator.getTaskNames()
    }

    @GetMapping()
    fun getTasks(query: TaskQuery): Mono<Paginated<UiTask>> {
        return coordinator.getPagedTasks(query)
    }

    @GetMapping("/{taskId}/reset")
    fun resetTask(@PathVariable taskId: UUID): Mono<ResponseEntity<ResetTaskResponse>> {
        return coordinator.resetTask(taskId)
            .map { ResponseEntity.ok(it) }
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode == HttpStatus.CONFLICT) {
                    Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                } else {
                    Mono.error(ex)
                }
            }
    }

    @GetMapping("/{taskId}/reset/force")
    fun resetTaskForce(@PathVariable taskId: UUID): Mono<ResponseEntity<ResetTaskResponse>> {
        return coordinator.resetTaskForced(taskId)
            .map { ResponseEntity.ok(it) }
            .onErrorResume(WebClientResponseException::class.java) { ex ->
                if (ex.statusCode == HttpStatus.CONFLICT) {
                    Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build())
                } else {
                    Mono.error(ex)
                }
            }
    }

    @GetMapping("/active")
    fun getActiveTasks() = coordinator.getActiveTasks()

    @GetMapping("/progress")
    fun getAllProgress(): Mono<List<Progress>> {
        return coordinator.getAllProgress()
    }

}

