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
    val log = KotlinLogging.logger {}

    fun getRequiredTasksForStartOperation(): Map<TaskStatusType, TaskStatus> {
        val taskStatuses = mutableMapOf<TaskStatusType, TaskStatus>()

        val startedWith = collect.startedWith?.tasks ?: emptyList()
        if (startedWith.any { it == OperationType.MetadataSearch }) {
            taskStatuses[TaskStatusType.MetadataSearch] = taskProjection.metadataTaskStatus
            taskStatuses[TaskStatusType.CoverDownload] = taskProjection.coverDownloadTaskStatus
        }
        if (startedWith.any { it == OperationType.ConvertSubtitles }) {
            taskStatuses[TaskStatusType.ConvertSubtitles] = taskProjection.convertTaskStatus
        }
        if (startedWith.any { it == OperationType.ExtractSubtitles }) {
            taskStatuses[TaskStatusType.ExtractSubtitles] = taskProjection.extractTaskStatus
            taskStatuses[TaskStatusType.DetermineCollection] = taskProjection.determineCollectionTaskStatus
            taskStatuses[TaskStatusType.ReadStreams] = taskProjection.readStreamsTaskStatus
            taskStatuses[TaskStatusType.PrepareForWork] = taskProjection.prepareForWorkTaskStatus
        }
        if (startedWith.any { it == OperationType.Encode }) {
            taskStatuses[TaskStatusType.Encode] = taskProjection.encodeTaskStatus
            taskStatuses[TaskStatusType.DetermineCollection] = taskProjection.determineCollectionTaskStatus
            taskStatuses[TaskStatusType.ReadStreams] = taskProjection.readStreamsTaskStatus
            taskStatuses[TaskStatusType.PrepareForWork] = taskProjection.prepareForWorkTaskStatus
        }
        return taskStatuses
    }

    /**
     * Intern hjelper som sjekker oppgavene og returnerer både om de er klare,
     * samt et kart over de som er i uønsket tilstand (NotInitiated eller Pending).
     */
    private fun evaluateRequiredTasks(): Pair<Boolean, Map<TaskStatusType, TaskStatus>> {
        val nonQualifiedToContinue = listOf(TaskStatus.NotInitiated, TaskStatus.Pending)
        val requiredTasks = getRequiredTasksForStartOperation()
        val undesiredState = requiredTasks.filter { it.value in nonQualifiedToContinue }

        return Pair(undesiredState.isEmpty(), undesiredState)
    }

    fun hasRequiredTasksToRunCompleted(): Boolean {
        return evaluateRequiredTasks().first
    }

    fun isWorkflowComplete(): Boolean {
        val statuses = taskProjection.operationStatuses()
        if (statuses.isEmpty()) return false

        if (statuses.any { it == TaskStatus.Failed } || statuses.any { it == TaskStatus.Pending }) {
            return false
        }

        return hasRequiredTasksToRunCompleted() && statuses.all { it == TaskStatus.Completed }
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

        // Bruker den delte hjelperen her for å få ut de uønskede statuene direkte
        val (isComplete, undesiredState) = evaluateRequiredTasks()
        if (!isComplete) {
            val taskNames = undesiredState.keys.map { it.name }.toSet()
            log.warn("Required tasks not complete for referenceId. Undesired states: $undesiredState")
            return WorkflowStatusReport(
                ReasonFailed(
                    reason = FailingReason.RequiredTasksAreNotComplete,
                    tasks = taskNames,
                    taskStatuses = undesiredState // Sender med hele map-en ut i rapporten
                )
            )
        }

        if (!isWorkflowComplete()) {
            return WorkflowStatusReport(ReasonFailed(FailingReason.RequiredTasksAreNotComplete))
        }

        return WorkflowStatusReport(reason = null)
    }
}