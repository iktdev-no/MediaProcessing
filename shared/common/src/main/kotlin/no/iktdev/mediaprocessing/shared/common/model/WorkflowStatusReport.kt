package no.iktdev.mediaprocessing.shared.common.model

import com.google.gson.Gson
import no.iktdev.eventi.models.store.TaskStatus

data class WorkflowStatusReport(
    val reason: Reason?
) {
    fun isFailed() = reason != null && reason is ReasonFailed
    fun getFailure(): ReasonFailed? = reason as? ReasonFailed
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

sealed class Reason() {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}
class ReasonFailed(val reason: FailingReason, val tasks: Set<String>? = emptySet(), val taskStatuses: Map<TaskStatusType, TaskStatus>? = emptyMap()) : Reason()

enum class FailingReason {
    MissingStart,
    NoRelevantTasksSet,
    TasksArePending,
    RequiredTasksHaveFailed,
    RequiredTasksAreNotComplete,
    RequiredOperationTasksHaveFailed
}