package no.iktdev.mediaprocessing.coordinator.controller


import no.iktdev.mediaprocessing.coordinator.CoordinatorService
import no.iktdev.mediaprocessing.coordinator.services.EventService
import no.iktdev.mediaprocessing.coordinator.services.ProgressTranslatorService
import no.iktdev.mediaprocessing.coordinator.services.TaskService
import no.iktdev.mediaprocessing.coordinator.toCoordinatorTransferDto
import no.iktdev.mediaprocessing.ffmpeg.util.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.ResetTaskResponse
import no.iktdev.mediaprocessing.shared.common.dto.TaskQuery
import no.iktdev.mediaprocessing.shared.common.dto.map
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CoordinatorTaskDto
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.progress.Progress
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
    private val eventService: EventService,
    private val coordinator: CoordinatorService,
    private val progressTranslatorService: ProgressTranslatorService
) {

    @GetMapping("/active")
    fun getActiveTasks(): List<CoordinatorTaskDto> {
        val tasks = taskService.getActiveTasks()
        val logEvents = eventService.getTaskEventResultsWithLogs(tasks.map { it.referenceId }.toSet())
        return tasks.map { it.toCoordinatorTransferDto(logEvents) }
    }

    @GetMapping
    fun getPagedTasks(query: TaskQuery): Paginated<CoordinatorTaskDto> {
        val paginatedTasks = taskService.getPagedTasks(query)
        val logEvents = eventService.getTaskEventResultsWithLogs(paginatedTasks.items.map { it.referenceId }.toSet())

        return paginatedTasks.map { it.toCoordinatorTransferDto(logEvents) }
    }



    @GetMapping("/{id}")
    fun getTask(@PathVariable id: UUID): CoordinatorTaskDto? {
        val tasks = taskService.getTaskById(id) ?: return null
        val logEvents = eventService.getTaskEventResultsWithLogs(setOf(tasks.referenceId))
        return tasks.toCoordinatorTransferDto(logEvents)
    }


    @GetMapping("/{taskId}/reset")
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
    @GetMapping("/{taskId}/reset/force")
    fun resetTaskForce(@PathVariable taskId: UUID): ResponseEntity<ResetTaskResponse> {
        return resetTask(taskId, true)
    }

    @GetMapping("/progress/all")
    fun getAllProgress(): List<Progress> {
        return coordinator.getProgress().map { progressTranslatorService.translate(it) }
    }

}
