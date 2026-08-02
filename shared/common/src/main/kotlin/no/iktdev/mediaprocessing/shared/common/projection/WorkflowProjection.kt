package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.model.*
import no.iktdev.mediaprocessing.shared.common.projection.tasks.TaskProjection

class WorkflowProjection(val events: List<Event>) {
    private val collect = CollectProjection(events)
    private val taskProjection = TaskProjection(events)

    fun hasRequiredTasksToRunCompleted(): Boolean {
        val nonQualifiedToContinue = listOf(TaskStatus.NotInitiated, TaskStatus.Pending)
        return listOf(
            taskProjection.determineCollectionTaskStatus,
            taskProjection.coverDownloadTaskStatus,
        ).none { it in nonQualifiedToContinue }
    }

    fun isWorkflowComplete(): Boolean {
        val statuses = taskProjection.operationStatuses()

        if (statuses.isEmpty()) return false

        val anyFailed = statuses.any { it == TaskStatus.Failed }
        val anyPending = statuses.any { it == TaskStatus.Pending }
        val allCompleted = statuses.all { it == TaskStatus.Completed }

        if (anyFailed) return false
        if (anyPending) return false

        if (!hasRequiredTasksToRunCompleted()) {
            return false
        }

        return allCompleted
    }

    fun evaluate(): WorkflowStatusReport {
        if (collect.startedWith == null) {
            return WorkflowStatusReport(ReasonFailed(FailingReason.MissingStart))
        }

        val statuses = taskProjection.operationStatuses()
        if (statuses.isEmpty()) {
            return WorkflowStatusReport(ReasonFailed(FailingReason.NoRelevantTasksSet))
        }

        if (taskProjection.hasFailed()) {
            val failedTasks = taskProjection.getStatuses()
                .filter { it.second == TaskStatus.Failed }
                .map { it.first.name }
                .toSet()
            return WorkflowStatusReport(ReasonFailed(FailingReason.RequiredTasksHaveFailed, failedTasks))
        }

        if (statuses.any { it == TaskStatus.Failed }) {
            return WorkflowStatusReport(ReasonFailed(FailingReason.RequiredOperationTasksHaveFailed))
        }

        if (statuses.any { it == TaskStatus.Pending }) {
            return WorkflowStatusReport(ReasonFailed(FailingReason.TasksArePending))
        }

        if (!hasRequiredTasksToRunCompleted()) {
            return WorkflowStatusReport(ReasonFailed(FailingReason.RequiredTasksAreNotComplete))
        }

        if (!isWorkflowComplete()) {
            return WorkflowStatusReport(ReasonFailed(FailingReason.RequiredTasksAreNotComplete))
        }

        return WorkflowStatusReport(reason = null)
    }
}