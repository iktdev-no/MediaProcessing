package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.EventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.requireAs
import no.iktdev.eventi.serialization.ZDS.toTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.AlterOverrideEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.AlteredOverrideEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component

@Component
class AlterOverrideEventListener(private val taskStore: TaskStore = TaskStore): EventListener() {
    private val log = KotlinLogging.logger {}

    override fun onEvent(
        event: Event,
        history: List<Event>
    ): Event? {
        val alterEvent = event.requireQualifiedEntry<AlterOverrideEvent>()
        val persistedTask = taskStore.findByTaskId(alterEvent.targetEventId)
        if (persistedTask == null) {
            log.error("Event ${alterEvent.targetEventId} not found in tasks")
            return null
        }
        val task = persistedTask.toTask() ?: run {
            log.error("Could not find task with id ${alterEvent.targetEventId}")
            return null
        }

        val success = when (task) {
            is MigrateToContentStoreTask -> applyOverrideToMigrateContentStoreTask(task ,alterEvent)
            else -> false
        }

        return if (success) {
            AlteredOverrideEvent(task.taskId)
        } else {
            log.warn("Could not apply alter override")
            null
        }
    }

    fun applyOverrideToMigrateContentStoreTask(task: MigrateToContentStoreTask, alterOverrideEvent: AlterOverrideEvent): Boolean {
        val overrides = alterOverrideEvent.overrides.mapNotNull { it -> try {
            MigrateToContentStoreTask.Overrides.valueOf(it)
        } catch (e: Exception) {
            e.printStackTrace()
            log.error("$it is unsupported for task ${task.taskId}")
            null
        }
        }
        task.overrides = overrides
        return taskStore.updateTask(task).also {
            if (it) {
                log.info("$task is updated")
            } else {
                log.error("$task is update failed")
            }
        }
    }
}