package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.FilePrepareForWorkTask
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection.TaskStatus
import java.util.*

class TaskProjection(val events: List<Event>) {

    private inline fun <reified C: Event, reified R: Event> projectStatus(
        crossinline createdIds: (List<C>) -> List<UUID>,
        crossinline resultStatus: (R) -> no.iktdev.eventi.models.store.TaskStatus,
        crossinline resultIds: (List<R>) -> List<UUID>
    ): TaskStatus {
        val createdEvent = events.getInstancesOf<C>().ifEmpty { return TaskStatus.NotInitiated }
        val resultEvent = events.getInstancesOf<R>().ifEmpty { return TaskStatus.Pending }

        val created = createdIds(createdEvent)
        val results = resultIds(resultEvent)

        val taskIds = results.filter { it in created }
        if (!taskIds.containsAll(created) || taskIds.size != created.size) return TaskStatus.Pending
        if (resultEvent.any { resultStatus(it) == no.iktdev.eventi.models.store.TaskStatus.Failed }) return TaskStatus.Failed

        return TaskStatus.Completed
    }


    fun projectStreamReadStatus() = projectStatus<CoordinatorReadStreamsTaskCreatedEvent, CoordinatorReadStreamsResultEvent>(
        createdIds = { it.map { e -> e.taskId } },
        resultStatus = { it.status },
        resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
    )

    fun projectCoverDownloadStatus() = projectStatus<CoverDownloadTaskCreatedEvent, CoverDownloadResultEvent>(
        createdIds = { it.flatMap { e -> e.taskIds } },
        resultStatus = { it.status },
        resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
    )

    fun projectMetadataSearchStatus() = projectStatus<MetadataSearchTaskCreatedEvent, MetadataSearchResultEvent>(
        createdIds = { it.map { e -> e.taskId } },
        resultStatus = { it.status },
        resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
    )

    fun projectEncodingPerformedStatus() = projectStatus<ProcesserEncodeTaskCreatedEvent, ProcesserEncodeResultEvent>(
        createdIds = { it.map { e -> e.taskId } },
        resultStatus = { it.status },
        resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
    )

    fun projectExtractSubtitleStatus(): TaskStatus {
        return projectStatus<ProcesserExtractTaskCreatedEvent, ProcesserExtractResultEvent>(
            createdIds = { it.flatMap { e -> e.taskIds } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )
    }

    fun projectConvertStatus(): TaskStatus {
        val baseStatus = projectStatus<ConvertTaskCreatedEvent, ConvertTaskResultEvent>(
            createdIds = { it.map { e -> e.taskId } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )

        val operations = events
            .filterIsInstance<StartProcessingEvent>()
            .lastOrNull()
            ?.data?.operation
            ?: emptySet()

        val hasExtractAndConvert = operations.contains(OperationType.ExtractSubtitles) &&
                operations.contains(OperationType.ConvertSubtitles)

        val hasCreatedConvert = events.filterIsInstance<ConvertTaskCreatedEvent>().isNotEmpty()

        return when {
            // Convert ikke en del av operasjonene → bruk baseStatus direkte
            !operations.contains(OperationType.ConvertSubtitles) -> baseStatus

            // Sekvensregel: både Extract og Convert er planlagt,
            // men ingen ConvertCreated finnes → Pending
            hasExtractAndConvert && !hasCreatedConvert -> TaskStatus.Pending

            // Ellers → baseStatus (Completed, Failed, Pending, NotInitiated)
            else -> baseStatus
        }
    }

    fun projectPrepareFileForWorkStatus(): TaskStatus {
        return projectStatus<FilePrepareForWorkTaskCreatedEvent, FilePrepareForWorkResultEvent>(
            createdIds = { it.map { e -> e.taskId } },
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )
    }

    fun projectMigrateContentStatus(): TaskStatus {
        return projectStatus<MigrateContentToStoreTaskCreatedEvent, MigrateContentToStoreTaskResultEvent>(
            createdIds = { it.map { e -> e.taskId }},
            resultStatus = { it.status },
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )
    }

    fun projectStoreContentAndMetadataStatus(): TaskStatus {
        return projectStatus<StoreContentAndMetadataTaskCreatedEvent, StoreContentAndMetadataTaskResultEvent>(
            createdIds = { it.map { e -> e.taskId }},
            resultStatus = {it.status},
            resultIds = { it.flatMap { e -> e.metadata.derivedFromId?.toList() ?: emptyList() } }
        )
    }

}