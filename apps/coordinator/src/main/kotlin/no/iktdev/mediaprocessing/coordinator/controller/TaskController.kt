package no.iktdev.mediaprocessing.coordinator.controller


import no.iktdev.mediaprocessing.coordinator.services.EventService
import no.iktdev.mediaprocessing.coordinator.services.TaskService
import no.iktdev.mediaprocessing.coordinator.translateDto.CoordinatorTaskTransferDto
import no.iktdev.mediaprocessing.coordinator.translateDto.toCoordinatorTransferDto
import no.iktdev.mediaprocessing.ffmpeg.util.UtcNow
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.ResetTaskResponse
import no.iktdev.mediaprocessing.shared.common.dto.TaskQuery
import no.iktdev.mediaprocessing.shared.common.dto.map
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
    private val eventService: EventService
) {

    @GetMapping("/active")
    fun getActiveTasks(): List<CoordinatorTaskTransferDto> =
        taskService.getActiveTasks().map { it.toCoordinatorTransferDto() }

    @GetMapping
    fun getPagedTasks(query: TaskQuery): Paginated<CoordinatorTaskTransferDto> {
        val paginatedTasks = taskService.getPagedTasks(query)
        return paginatedTasks.map { it.toCoordinatorTransferDto() }
    }



    @GetMapping("/{id}")
    fun getTask(@PathVariable id: UUID): CoordinatorTaskTransferDto? =
        taskService.getTaskById(id)?.toCoordinatorTransferDto()


    @GetMapping("/{taskId}/reset")
    fun resetTask(@PathVariable taskId: UUID, forced: Boolean = false): ResponseEntity<ResetTaskResponse> {
        val task = taskService.getTaskById(taskId)
            ?: return ResponseEntity.notFound().build()

        val referenceId = task.referenceId

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

}
