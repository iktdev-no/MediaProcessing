package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.mediaprocessing.shared.common.dto.PagedTasks
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Service
import java.util.*


@Service
class TaskService {


    fun getActiveTasks(): List<PersistedTask> {
        return TaskStore.findActiveTasks()
    }

    fun getPagedTasks(page: Int, size: Int): PagedTasks {
        return TaskStore.getPagedTasks(page, size)
    }

    fun getTaskById(taskId: UUID): PersistedTask? {
        return TaskStore.findByTaskId(taskId)
    }
}