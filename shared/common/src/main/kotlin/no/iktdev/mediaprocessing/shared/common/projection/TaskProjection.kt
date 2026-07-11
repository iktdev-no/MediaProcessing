package no.iktdev.mediaprocessing.shared.common.projection

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.TransferContentTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events_super.TransferredBaseResultEvent
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.projection.CollectProjection.TaskStatus
import java.util.*

class TaskProjection(val events: List<Event>) {
    val log = KotlinLogging.logger {}


    private inline fun <reified C : Event, reified R : Event> projectStatus(
        crossinline createdIds: (List<C>) -> List<UUID>,
        crossinline resultStatus: (R) -> no.iktdev.eventi.models.store.TaskStatus,
        crossinline resultIds: (List<R>) -> List<UUID>
    ): TaskStatus {
        val createdEvent = events.getInstancesOf<C>()
        if (createdEvent.isEmpty()) {
            log.debug { "[Projection] NotInitiated: No instances of ${C::class.simpleName} found in history." }
            return TaskStatus.NotInitiated
        }

        val resultEvent = events.getInstancesOf<R>()
        if (resultEvent.isEmpty()) {
            log.debug { "[Projection] Pending: Found ${createdEvent.size} created events, but zero result events of type ${R::class.simpleName}." }
            return TaskStatus.Pending
        }

        val created = createdIds(createdEvent)
        val results = resultIds(resultEvent)

        // Sjekk 1: Mangler vi resultater for spesifikke oppgaver?
        if (!results.containsAll(created)) {
            val missingIds = created.filter { it !in results }
            log.debug {
                "[Projection] Pending: Missing results for ${missingIds.size} tasks. " +
                        "Created IDs: $created | Result mapping IDs: $results | Missing: $missingIds"
            }
            return TaskStatus.Pending
        }

        // Sjekk 2: Har noen av resultatene feilet?
        val failedResults = resultEvent.filter { resultStatus(it) == no.iktdev.eventi.models.store.TaskStatus.Failed }
        if (failedResults.isNotEmpty()) {
            log.debug {
                "[Projection] Failed: ${failedResults.size} result events had status Failed. " +
                        "Failed event IDs: ${failedResults.map { it.eventId }}"
            }
            return TaskStatus.Failed
        }

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
