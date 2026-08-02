package no.iktdev.mediaprocessing.shared.common.model

data class WorkflowStatusReport(
    val reason: Reason?
) {
    fun isFailed() = reason != null && reason is ReasonFailed
}

sealed class Reason()
class ReasonFailed(val reason: FailingReason, val tasks: Set<String>? = emptySet()) : Reason()

enum class FailingReason {
    MissingStart,
    NoRelevantTasksSet,
    TasksArePending,
    RequiredTasksHaveFailed,
    RequiredTasksAreNotComplete,
    RequiredOperationTasksHaveFailed
}