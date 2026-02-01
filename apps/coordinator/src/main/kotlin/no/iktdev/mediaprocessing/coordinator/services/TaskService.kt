package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.mediaprocessing.shared.common.dto.Paginated
import no.iktdev.mediaprocessing.shared.common.dto.TaskQuery
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Service
import java.util.*


@Service
class TaskService(
    private val eventService: EventService
) {


    fun getActiveTasks(): List<PersistedTask> {
        return getNonDeleted(TaskStore.findActiveTasks())
    }

    fun getPagedTasks(page: TaskQuery): Paginated<PersistedTask> {
        return TaskStore.getPagedTasks(page).let {
            it.copy(items = getNonDeleted(it.items))
        }
    }

    fun getTaskById(taskId: UUID): PersistedTask? {
        val task = TaskStore.findByTaskId(taskId) ?: return null
        return if (eventService.isSequenceDeleted(task.referenceId)) null else task
    }

    fun resetFailedTask(taskId: UUID): Boolean {
        val resetSuccess = TaskStore.resetTaskById(taskId).isSuccess
        return resetSuccess
    }

    fun getFailedTasks(): List<PersistedTask> {
        return getNonDeleted(TaskStore.getFailedTasks())
    }

    private fun getNonDeleted(tasks: List<PersistedTask>): List<PersistedTask> {
        if (tasks.isEmpty()) return emptyList()

        // 1. Finn alle referenceId i batchen
        val referenceIds = tasks.map { it.referenceId }.toSet()

        // 2. Slå opp alle slettede sekvenser i ett kall
        val deleted = eventService.getDeletedSequences(referenceIds)

        // 3. Filtrer bort tasks som tilhører slettede sekvenser
        return tasks.filterNot { it.referenceId in deleted }
    }

}