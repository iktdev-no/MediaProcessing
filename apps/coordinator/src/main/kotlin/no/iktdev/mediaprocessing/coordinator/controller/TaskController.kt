package no.iktdev.mediaprocessing.coordinator.controller


import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.mediaprocessing.coordinator.services.TaskService
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.TaskQuery
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
    fun getPagedTasks(query: TaskQuery): Paginated<PersistedTask> =
        taskService.getPagedTasks(query)



    @GetMapping("/{id}")
    fun getTask(@PathVariable id: UUID): PersistedTask? =
        taskService.getTaskById(id)
}
