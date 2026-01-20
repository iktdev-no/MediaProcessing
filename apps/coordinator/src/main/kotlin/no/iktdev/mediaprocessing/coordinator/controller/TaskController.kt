package no.iktdev.mediaprocessing.coordinator.controller


import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.mediaprocessing.coordinator.services.TaskService
import no.iktdev.mediaprocessing.shared.common.dto.PagedTasks
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/tasks")
class TaskController(
    private val taskService: TaskService,
) {

    @GetMapping("/active")
    fun getActiveTasks(): List<PersistedTask> =
        taskService.getActiveTasks()

    @GetMapping
    fun getPagedTasks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): PagedTasks =
        taskService.getPagedTasks(page, size)

    @GetMapping("/{id}")
    fun getTask(@PathVariable id: UUID): PersistedTask? =
        taskService.getTaskById(id)
}
