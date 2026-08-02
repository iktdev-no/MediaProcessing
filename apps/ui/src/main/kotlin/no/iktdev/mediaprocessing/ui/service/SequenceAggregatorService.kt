package no.iktdev.mediaprocessing.ui.service

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskCreatedEvent
import no.iktdev.eventi.models.SingleTaskCreatedEvent
import no.iktdev.eventi.models.TaskCreatedEvent
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.effective
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CompletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.model.ReasonFailed
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection
import no.iktdev.mediaprocessing.shared.common.projection.CollectionProjection
import no.iktdev.mediaprocessing.shared.common.projection.SignalProjection
import no.iktdev.mediaprocessing.shared.common.projection.SummaryProjection
import no.iktdev.mediaprocessing.shared.common.projection.WorkflowProjection
import no.iktdev.mediaprocessing.shared.common.projection.tasks.TaskProjection
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.ui.models.contract.MediaType
import no.iktdev.mediaprocessing.ui.models.contract.sequence.LifecycleNode
import no.iktdev.mediaprocessing.ui.models.contract.sequence.LifecycleNodeType
import no.iktdev.mediaprocessing.ui.models.contract.sequence.TaskLifecycleItem
import no.iktdev.mediaprocessing.ui.models.contract.sequence.CurrentState
import no.iktdev.mediaprocessing.ui.models.contract.sequence.Mode
import no.iktdev.mediaprocessing.ui.models.contract.sequence.Sequence
import no.iktdev.mediaprocessing.ui.models.contract.sequence.SequenceSummary
import no.iktdev.mediaprocessing.ui.models.contract.sequence.TaskType
import no.iktdev.mediaprocessing.ui.models.contract.toUiMediaType
import no.iktdev.mediaprocessing.ui.models.contract.toUiTaskStatus
import no.iktdev.mediaprocessing.ui.models.translate
import no.iktdev.mediaprocessing.ui.toEvents
import no.iktdev.mediaprocessing.ui.toUIEvent
import no.iktdev.mediaprocessing.ui.toUITask
import no.iktdev.mediaprocessing.ui.translate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class SequenceAggregatorService(
    private val eventService: EventService,
    private val taskService: TaskService,
) {
    fun getActiveSequences(): List<Sequence> {
        val allEvents = EventStore.getPersistedEventsAfter(Instant.EPOCH)
        return getSequences(allEvents,
            { group: List<PersistedEvent> ->
                group.none { it.event == CompletedEvent::class.java.simpleName }
            }
        )
    }

    fun getRecentSequences(limit: Int): List<Sequence> {
        val allEvents = EventStore.getPersistedEventsAfter(Instant.EPOCH)
        return getSequences(allEvents).take(limit)
    }

    fun getSequences(
        events: List<PersistedEvent>,
        vararg groupFilters: (List<PersistedEvent>) -> Boolean
    ): List<Sequence> {

        val grouped = events
            .groupBy { it.referenceId }
            // filtrer grupper før composeSummary
            .filter { (_, group) -> groupFilters.all { filter -> filter(group) } }
            // bygg summary
            .mapNotNull { (id, group) ->
                composeSummary(group)?.let { summary -> id to summary }
            }
            .toMap()

        val deleted = eventService.getDeletedSequences(grouped.keys)

        return grouped
            .filterNot { (referenceId, _) -> referenceId in deleted }
            .values
            .sortedByDescending { it.lastEventTime }
    }



    fun getTitle(pe: List<PersistedEvent>): String? {
        val events = pe.mapNotNull { it.toEvent() }
        val projection = CollectProjection(events)
        return projection.parsedFileInfo?.name ?: projection.metadata?.title
    }

    fun composeSummary(persisted: List<PersistedEvent>): Sequence? {
        val last = persisted.maxByOrNull { it.persistedAt } ?: return null
        val events = persisted.mapNotNull { it.toEvent() }
        val signals = SignalProjection(events)

        val domainEvents = events.effective()
        val projection = CollectProjection(domainEvents)
        val taskProjection = TaskProjection(domainEvents)

        val state = if (signals.isReleased) {
            CurrentState.Continuing
        } else if (signals.isOnHold) {
            CurrentState.OnHold
        } else {
            CurrentState.Continuing
        }

        return Sequence(
            referenceId = last.referenceId.toString(),
            title = getTitle(persisted) ?: "",
            inputFileName = projection.useFile?.name,
            lastEventId = last.eventId.toString(),
            lastEventTime = last.persistedAt,
            tasks = mapOf(
                TaskType.ReadStreams to taskProjection.readStreamsTaskStatus.toUiTaskStatus(),
                TaskType.MetadataSearch to taskProjection.metadataTaskStatus.toUiTaskStatus(),
                TaskType.Encode to taskProjection.encodeTaskStatus.toUiTaskStatus(),
                TaskType.SubtitleExtract to taskProjection.extreactTaskStatus.toUiTaskStatus(),
                TaskType.SubtitleConvert to taskProjection.convertTaskStatus.toUiTaskStatus(),
                TaskType.CoverDownload to taskProjection.coverDownloadTaskStatus.toUiTaskStatus(),
                TaskType.ContentPersist to taskProjection.contentMigratedTaskStatus.toUiTaskStatus(),
                TaskType.MediaInfoStored to taskProjection.contentStoredTaskStatus.toUiTaskStatus()
            ),
            mode = when (projection.startedWith?.mode) {
                StartFlow.Auto -> Mode.Auto
                StartFlow.Manual -> Mode.Manual
                else -> Mode.Auto
            },
            currentState = state,
            hasErrors = taskProjection.hasFailed()
        )
    }



    fun Pair<PersistedTask, List<TaskResultEvent>>.toLifecycleItem(): TaskLifecycleItem {
        val logs = this.second.mapNotNull { it.logFile }
        val task = this.first.toUITask(logs)
        return TaskLifecycleItem(
            taskId = task.taskId,
            task,
            this.second.map { it.toUIEvent() }
        )
    }

    fun generateEffectiveLifecycle(refId: UUID): List<LifecycleNode> {
        val events = eventService.getEffectiveHistory(refId).toEvents()
        val tasks = taskService.getTasksByReferenceId(refId)

        return generateLifecycle(events, tasks)
    }

    fun generateLifecycle(events: List<Event>, tasks: List<PersistedTask>): List<LifecycleNode> {
        val lifecycles: MutableList<LifecycleNode> = mutableListOf()


        val resultEvents = events.getInstancesOf<TaskResultEvent>()
        events.filterIsInstance<TaskCreatedEvent>().forEach { createdEvent ->

            val taskIds = when (createdEvent) {
                is SingleTaskCreatedEvent -> listOf(createdEvent.taskId)
                is MultiTaskCreatedEvent -> createdEvent.taskIds.map { it.taskId }
                else -> emptyList()
            }

            val createdTasks = tasks.filter { it.taskId in taskIds }.map { task ->
                val relevantResults = resultEvents.filter { task.taskId in (it.metadata.derivedFromId ?: emptySet()) }
                task to relevantResults
            }

            lifecycles.add(LifecycleNode(
                lifecycleId = createdEvent.eventId,
                referenceId = createdEvent.referenceId,
                type = LifecycleNodeType.EventTaskGroup,
                event = null,
                taskOwnerEvent = createdEvent.toUIEvent(),
                tasks = createdTasks.map { ct ->
                    ct.toLifecycleItem()
                }
            ))

        }

        events.filter { it !is TaskCreatedEvent && it !is TaskResultEvent }.forEach { event ->
            lifecycles.add(LifecycleNode(
                lifecycleId = event.eventId,
                referenceId = event.referenceId,
                type = LifecycleNodeType.Event,
                event = event.toUIEvent(),
                taskOwnerEvent = null,
            ))
        }

        return lifecycles.sortedBy { node ->
            node.taskOwnerEvent?.persistedAt ?: node.event?.persistedAt
        }
    }

    fun getSequenceSummary(refId: UUID): SequenceSummary {
        val events = eventService.getEffectiveHistory(refId).toEvents()
        val workflow = WorkflowProjection(events)
        val statusReport = workflow.evaluate()

        val collect = CollectProjection(events)
        val summaryProjection = SummaryProjection(
            collection = collect.parsedFileInfo?.collection ?: "Unknown",
            events = events,
            outbox = IFile("")
        )

        val reasonFailed = statusReport.reason as? ReasonFailed

        // Utled tittel, mediatype og episodeinfo som før
        val title = collect.metadata?.title
            ?: collect.parsedFileInfo?.name
            ?: summaryProjection.getFileName()

        val mediaType = collect.metadata?.mediaType
            ?: collect.parsedFileInfo?.mediaType

        val epInfo = summaryProjection.projectEpisodeInfo()
        val episodeSummary = epInfo?.let {
            SequenceSummary.EpisodeInfoSummary(
                seasonNumber = it.seasonNumber,
                episodeNumber = it.episodeNumber,
                episodeTitle = it.episodeTitle
            )
        }

        // Hent ut den avleste metadataen fra CollectProjection
        val metadataSummary = collect.metadata?.let { meta ->
            SequenceSummary.MetadataSummary(
                title = meta.title,
                alternativeTitles = meta.alternativeTitles,
                genres = meta.genres,
                source = meta.source,
                hasCover = meta.cover != null
            )
        }

        val targetCollection = try {
            CollectionProjection(events).getCollection()
        } catch (e: Exception) {
            null
        }

        return SequenceSummary(
            title = title,
            collection = targetCollection,
            mediaType = mediaType?.toUiMediaType(),
            episodeInfo = episodeSummary,
            metadata = metadataSummary, // Sendes med til UI
            failingReasons = reasonFailed?.reason?.translate(),
            failedTasks = reasonFailed?.tasks ?: emptySet(),
        )
    }


}