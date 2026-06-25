package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.TransferContentTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events_super.TransferredBaseResultEvent
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection.TaskStatus
import java.util.*

class TaskProjection(val events: List<Event>) {

    private inline fun <reified C : Event, reified R : Event> projectStatus(
        crossinline createdIds: (List<C>) -> List<UUID>,
        crossinline resultStatus: (R) -> no.iktdev.eventi.models.store.TaskStatus,
        crossinline resultIds: (List<R>) -> List<UUID>
    ): TaskStatus {

        val createdEvent = events.getInstancesOf<C>().ifEmpty { return TaskStatus.NotInitiated }
        val resultEvent = events.getInstancesOf<R>().ifEmpty { return TaskStatus.Pending }

        val created = createdIds(createdEvent)
        val results = resultIds(resultEvent)

        // Match resultater som faktisk hører til created-taskene
        val matching = results.filter { it in created }

        // Ikke alle created-tasks har resultater → Pending
        if (!matching.containsAll(created) || matching.size != created.size)
            return TaskStatus.Pending

        // Noen resultater har status Failed → Failed
        if (resultEvent.any { resultStatus(it) == no.iktdev.eventi.models.store.TaskStatus.Failed })
            return TaskStatus.Failed

        return TaskStatus.Completed
    }

    // 1: Stream read (én taskId)
    fun projectStreamReadStatus() =
        projectStatus<CoordinatorReadStreamsTaskCreatedEvent, CoordinatorReadStreamsResultEvent>(
            createdIds = { it.map { e -> e.taskId } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )

    // 2: Cover download (flere taskIds)
    fun projectCoverDownloadStatus(): TaskStatus {
        if (events.findLast { it is CoverDownloadSkippedEvent } != null) {
            return TaskStatus.Skipped
        }
        return projectStatus<CoverDownloadTaskCreatedEvent, CoverDownloadResultEvent>(
            createdIds = { it.flatMap { e -> e.taskIds.map { v -> v.taskId } }},
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )
    }

    // 3: Metadata search (én taskId)
    fun projectMetadataSearchStatus() =
        projectStatus<MetadataSearchTaskCreatedEvent, MetadataSearchResultEvent>(
            createdIds = { it.map { e -> e.taskId } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )

    // 4: Encoding (én taskId)
    fun projectEncodingPerformedStatus() =
        projectStatus<ProcesserEncodeTaskCreatedEvent, ProcesserEncodeResultEvent>(
            createdIds = { it.map { e -> e.taskId } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )

    // 5: Extract subtitles (flere taskIds)
    fun projectExtractSubtitleStatus() =
        projectStatus<ProcesserExtractTaskCreatedEvent, ProcesserExtractResultEvent>(
            createdIds = { it.flatMap { e -> e.taskIds.map { v -> v.taskId } }},
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )

    // 6: Convert (én taskId)
    fun projectConvertStatus(): TaskStatus {
        val baseStatus =
            projectStatus<ConvertTaskCreatedEvent, ConvertTaskResultEvent>(
                createdIds = { it.map { e -> e.taskId } },
                resultStatus = { it.status },
                resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
            )

        val operations = events
            .filterIsInstance<StartProcessingEvent>()
            .lastOrNull()
            ?.data?.operation
            ?: emptySet()

        val convertPlanned = operations.contains(OperationType.ConvertSubtitles)
        val extractPlanned = operations.contains(OperationType.ExtractSubtitles)

        val pipelineMode = convertPlanned && extractPlanned
        val standaloneMode = convertPlanned && !extractPlanned

        val extractStarted =
            events.any { it is ProcesserExtractTaskCreatedEvent } ||
                    events.any { it is ProcesserExtractResultEvent }

        val convertCreated =
            events.any { it is ConvertTaskCreatedEvent }

        return when {
            !convertPlanned -> baseStatus

            pipelineMode && extractStarted && !convertCreated ->
                TaskStatus.Pending

            standaloneMode && !convertCreated ->
                baseStatus

            else -> baseStatus
        }
    }


    // 7: Prepare file for work (én taskId)
    fun projectPrepareFileForWorkStatus() =
        projectStatus<FilePrepareForWorkTaskCreatedEvent, FilePrepareForWorkResultEvent>(
            createdIds = { it.map { e -> e.taskId } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )

    // 8: Migrate content (én taskId)
    fun projectMigrateContentStatus() =
        projectStatus<TransferContentTaskCreatedEvent, TransferredBaseResultEvent>(
            createdIds = { it.flatMap { e -> e.taskIds.map { v -> v.taskId } }},
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )

    // 9: Store content + metadata (én taskId)
    fun projectStoreContentAndMetadataStatus() =
        projectStatus<StoreMediaInfoAndMetadataTaskCreatedEvent, StoreMediaInfoAndMetadataTaskResultEvent>(
            createdIds = { it.map { e -> e.taskId } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )

    fun projectDeterminedCollectionStatus() =
        projectStatus<DetermineCollectionTaskCreatedEvent, DeterminedCollectionTaskResultEvent>(
            createdIds = { it.map { e -> e.taskId } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )
}
