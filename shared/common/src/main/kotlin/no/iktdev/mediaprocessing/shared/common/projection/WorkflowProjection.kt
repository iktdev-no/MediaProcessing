package no.iktdev.mediaprocessing.shared.common.projection

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.model.*
import no.iktdev.mediaprocessing.shared.common.projection.tasks.TaskProjection

class WorkflowProjection(val events: List<Event>) {
    private val collect = CollectProjection(events)
    private val taskProjection = TaskProjection(events)
    private val log = KotlinLogging.logger {}

    fun getRequiredTasksForStartOperation(): Map<TaskStatusType, TaskStatus> {
        val taskStatuses = mutableMapOf<TaskStatusType, TaskStatus>()
        val startedWith = collect.startedWith?.tasks ?: emptyList()

        if (OperationType.MetadataSearch in startedWith) {
            taskStatuses[TaskStatusType.MetadataSearch] = taskProjection.metadataTaskStatus
            taskStatuses[TaskStatusType.CoverDownload] = taskProjection.coverDownloadTaskStatus
        }

        if (OperationType.ConvertSubtitles in startedWith) {
            taskStatuses[TaskStatusType.ConvertSubtitles] = taskProjection.convertTaskStatus
        }

        if (OperationType.ExtractSubtitles in startedWith) {
            taskStatuses[TaskStatusType.ExtractSubtitles] = taskProjection.extractTaskStatus
            taskStatuses[TaskStatusType.DetermineCollection] = taskProjection.determineCollectionTaskStatus
            taskStatuses[TaskStatusType.ReadStreams] = taskProjection.readStreamsTaskStatus
            taskStatuses[TaskStatusType.PrepareForWork] = taskProjection.prepareForWorkTaskStatus
        }

        if (OperationType.Encode in startedWith) {
            taskStatuses[TaskStatusType.Encode] = taskProjection.encodeTaskStatus
            taskStatuses[TaskStatusType.DetermineCollection] = taskProjection.determineCollectionTaskStatus
            taskStatuses[TaskStatusType.ReadStreams] = taskProjection.readStreamsTaskStatus
            taskStatuses[TaskStatusType.PrepareForWork] = taskProjection.prepareForWorkTaskStatus
        }

        return taskStatuses
    }

    private fun getInvalidRequiredTasks(): Map<TaskStatusType, TaskStatus> {
        return getRequiredTasksForStartOperation()
            .filter { (_, status) ->
                status != TaskStatus.Completed && status != TaskStatus.Skipped
            }
    }

    fun hasRequiredTasksToRunCompleted(): Boolean {
        return getInvalidRequiredTasks().isEmpty()
    }

    fun isWorkflowComplete(): Boolean {
        if (collect.startedWith == null) return false

        val requiredTasks = getRequiredTasksForStartOperation()
        if (requiredTasks.isEmpty()) return false

        if (requiredTasks.values.any { it != TaskStatus.Completed && it != TaskStatus.Skipped }) {
            return false
        }

        val operations = taskProjection.operationStatuses()
        return operations.isNotEmpty() &&
                operations.all { it == TaskStatus.Completed || it == TaskStatus.Skipped }
    }

    fun evaluate(): WorkflowStatusReport {
        if (collect.startedWith == null) {
            return WorkflowStatusReport(
                ReasonFailed(FailingReason.MissingStart)
            )
        }

        val requiredTasks = getRequiredTasksForStartOperation()
        if (requiredTasks.isEmpty()) {
            return WorkflowStatusReport(
                ReasonFailed(FailingReason.NoRelevantTasksSet)
            )
        }

        val invalidRequiredTasks = getInvalidRequiredTasks()

        if (invalidRequiredTasks.isNotEmpty()) {
            val failed = invalidRequiredTasks
                .filter { it.value == TaskStatus.Failed }

            if (failed.isNotEmpty()) {
                return WorkflowStatusReport(
                    ReasonFailed(
                        reason = FailingReason.RequiredTasksHaveFailed,
                        tasks = failed.keys.map { it.name }.toSet(),
                        taskStatuses = failed
                    )
                )
            }

            val pending = invalidRequiredTasks
                .filter { it.value == TaskStatus.Pending }

            if (pending.isNotEmpty()) {
                return WorkflowStatusReport(
                    ReasonFailed(
                        reason = FailingReason.TasksArePending,
                        tasks = pending.keys.map { it.name }.toSet(),
                        taskStatuses = pending
                    )
                )
            }

            return WorkflowStatusReport(
                ReasonFailed(
                    reason = FailingReason.RequiredTasksAreNotComplete,
                    tasks = invalidRequiredTasks.keys.map { it.name }.toSet(),
                    taskStatuses = invalidRequiredTasks
                )
            )
        }

        return WorkflowStatusReport(reason = null)
    }
}