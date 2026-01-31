package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.TaskQuery
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Service
import java.util.*


@Service
class TaskService {


    fun getActiveTasks(): List<PersistedTask> {
        return TaskStore.findActiveTasks()
    }

    fun getPagedTasks(page: TaskQuery): Paginated<PersistedTask> {
        return TaskStore.getPagedTasks(page)
    }

    fun getTaskById(taskId: UUID): PersistedTask? {
        return TaskStore.findByTaskId(taskId)
    }

    fun resetFailedTask(taskId: UUID): Boolean {
        val resetSuccess = TaskStore.resetTaskById(taskId).isSuccess
        return resetSuccess
    }

    fun getFailedTasks(): List<PersistedTask> {
        return TaskStore.getFailedTasks()
    }
}