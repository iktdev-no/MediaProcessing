package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus


class MetadataSearchTaskCreated(): Event() {}

class MetadataSearchTaskPerformed(
    val data: pyMetadata? = null,
    val taskStatus: TaskStatus
): Event() {
    init {
        assert(taskStatus in listOf(TaskStatus.Completed, TaskStatus.Failed), { "Task status is not of acceptable state $taskStatus" })
    }
}

data class pyMetadata(
    val title: String,
    val altTitle: List<String> = emptyList(),
    val cover: String? = null,
    val type: String,
    val summary: List<pySummary> = emptyList(),
    val genres: List<String> = emptyList()
)

data class pySummary(
    val summary: String?,
    val language: String = "eng"
)