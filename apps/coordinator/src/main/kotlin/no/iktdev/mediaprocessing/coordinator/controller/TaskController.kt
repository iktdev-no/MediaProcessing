package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.services.EventService
import no.iktdev.mediaprocessing.coordinator.services.ProgressManagerService
import no.iktdev.mediaprocessing.coordinator.services.TaskService
import no.iktdev.mediaprocessing.ffmpeg.util.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.IgnoredTaskResponse
import no.iktdev.mediaprocessing.shared.common.dto.ResetTaskResponse
import no.iktdev.mediaprocessing.shared.common.dto.progress.Progress
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
    private val eventService: EventService,
    private val progressManagerService: ProgressManagerService
) {

    @GetMapping("/taskid/{taskId}/reset")
    fun resetTask(@PathVariable taskId: UUID, forced: Boolean = false): ResponseEntity<ResetTaskResponse> {
        val task = taskService.getTaskById(taskId)
            ?: return ResponseEntity.notFound().build()

        val referenceId = task.referenceId
        if (eventService.isSequenceDeleted(referenceId)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build()
        }

        // 1. Opprett DeleteEvent
        val deletedId = eventService.deleteTaskFailureForReset(referenceId, taskId)
        if (deletedId == null) {
            if (forced) {
                eventService.createForcedTaskResetAuditEvent(referenceId, taskId)
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).build()
            }
        }

        // 2. Reset task
        val success = taskService.resetFailedTask(taskId)

        return ResponseEntity.ok(
            ResetTaskResponse(
                taskId = taskId,
                referenceId = referenceId,
                reset = success,
                deletedEventId = deletedId,
                resetAt = UtcNow()
            )
        )
    }

    @PatchMapping("/taskid/{taskId}/override")
    fun setTaskOverrides(@PathVariable taskId: UUID, @RequestBody overrides: List<String>): Mono<Boolean> {
        return Mono.just(taskService.setTaskOverrides(taskId, overrides))
    }

    @PatchMapping("/taskid/{taskId}/ignore")
    fun setTaskIgnore(@PathVariable taskId: UUID): ResponseEntity<IgnoredTaskResponse> {
        val task = taskService.getTaskById(taskId)
            ?: return ResponseEntity.notFound().build()

        val referenceId = task.referenceId
        if (eventService.isSequenceDeleted(referenceId)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build()
        }

        val success = eventService.deleteTaskResultForIgnore(referenceId, taskId)
        val fullyUpdated = taskService.markTaskAsSkipped(taskId)

        return ResponseEntity.ok(
            IgnoredTaskResponse(
                taskId = taskId,
                referenceId = referenceId,
                skipped = success != null && fullyUpdated,
                deletedEventId = success?.first,
                skippedEventId = success?.second,
                ignoredAt = UtcNow()
            )
        )
    }

    @GetMapping("/taskid/{taskId}/reset/force")
    fun resetTaskForce(@PathVariable taskId: UUID): ResponseEntity<ResetTaskResponse> {
        return resetTask(taskId, true)
    }


    @GetMapping("/progress/all")
    fun getAllProgress(): List<Progress> {
        return progressManagerService.getProgress()
    }

}
