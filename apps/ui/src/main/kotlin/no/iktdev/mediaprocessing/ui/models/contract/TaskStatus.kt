package no.iktdev.mediaprocessing.ui.models.contract

import no.iktdev.eventi.models.store.TaskStatus as EventiTaskStatus

enum class TaskStatus {
    NotInitiated,
    Pending,
    InProgress,
    Completed,
    Failed,
    Cancelled,
    Skipped
}


fun EventiTaskStatus.toUiTaskStatus(): TaskStatus {
    return TaskStatus.valueOf(this.name)
}