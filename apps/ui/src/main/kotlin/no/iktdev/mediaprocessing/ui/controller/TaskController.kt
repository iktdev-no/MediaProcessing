package no.iktdev.mediaprocessing.ui.controller

import no.iktdev.mediaprocessing.ffmpeg.util.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.IgnoredTaskResponse
import no.iktdev.mediaprocessing.shared.common.dto.ResetTaskResponse
import no.iktdev.mediaprocessing.shared.common.dto.query.TaskQuery
import no.iktdev.mediaprocessing.shared.common.dto.map
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskRegistry
import no.iktdev.mediaprocessing.ui.models.contract.UiTask
import no.iktdev.mediaprocessing.ui.models.contract.Paginated
import no.iktdev.mediaprocessing.ui.models.contract.toUi
import no.iktdev.mediaprocessing.ui.service.EventService
import no.iktdev.mediaprocessing.ui.service.TaskService
import no.iktdev.mediaprocessing.ui.toCoordinatorTransferDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/tasks")
class TaskController(
    private val taskService: TaskService,
    private val eventService: EventService,
) {


    @GetMapping("/names")
    fun getTaskNames(): List<String> {
        return TaskRegistry.getTasks().map { it.simpleName }
    }


    @GetMapping("/active")
    fun getActiveTasks(): List<UiTask> {
        val tasks = taskService.getActiveTasks()
        val logEvents = eventService.getTaskEventResultsWithLogs(tasks.map { it.referenceId }.toSet())
        return tasks.map { it.toCoordinatorTransferDto(logEvents) }
    }

    @GetMapping
    fun getPagedTasks(query: TaskQuery): Paginated<UiTask> {
        val paginatedTasks = taskService.getPagedTasks(query)
        val logEvents = eventService.getTaskEventResultsWithLogs(paginatedTasks.items.map { it.referenceId }.toSet())

        return paginatedTasks.map { it.toCoordinatorTransferDto(logEvents) }.toUi()
    }

    @GetMapping("/by-reference/{referenceId}")
    fun getTaskByReferenceId(@PathVariable referenceId: UUID): List<UiTask> {
        val tasks = taskService.getTasksByReferenceId(referenceId)
        val logEvents = eventService.getTaskEventResultsWithLogs(tasks.map { it.referenceId }.toSet())
        return tasks.map { it.toCoordinatorTransferDto(logEvents) }
    }

    @GetMapping("/taskid/{id}")
    fun getTask(@PathVariable id: UUID): UiTask? {
        val tasks = taskService.getTaskById(id) ?: return null
        val logEvents = eventService.getTaskEventResultsWithLogs(setOf(tasks.referenceId))
        return tasks.toCoordinatorTransferDto(logEvents)
    }


    @GetMapping("/taskid/{taskId}/reset")
    fun resetTask(@PathVariable taskId: UUID, forced: Boolean = false): ResponseEntity<ResetTaskResponse> {
        val task = taskService.getTaskById(taskId)
            ?: return ResponseEntity.notFound().build()

        val referenceId = task.referenceId
        if (eventService.isSequenceDeleted(referenceId)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build()
        }

        // 1. Opprett DeleteEvent
        val deletedId = eventService.deleteTaskResultForReset(referenceId, taskId)
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
    fun setTaskOverrides(@PathVariable taskId: UUID, @RequestBody overrides: List<String>): Boolean {
        return taskService.setTaskOverrides(taskId, overrides)
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

}
