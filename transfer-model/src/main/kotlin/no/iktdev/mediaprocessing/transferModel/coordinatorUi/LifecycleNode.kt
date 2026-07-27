package no.iktdev.mediaprocessing.transferModel.coordinatorUi

import java.util.UUID

data class LifecycleNode(
    val lifecycleId: UUID, // Will be ordinaryEventId or taskCreatedEventId
    val referenceId: UUID,
    val type: LifecycleNodeType,

    val event: UiEvent?,
    val taskOwnerEvent: UiEvent?,
    val tasks: List<TaskLifecycleItem> = emptyList()
) {
}

data class TaskLifecycleItem(
    val taskId: UUID,
    val task: CoordinatorTaskDto?,
    val taskResultEvents: List<UiEvent> = emptyList() // Kan også være flere resultater over tid
)