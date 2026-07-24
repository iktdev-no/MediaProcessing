package no.iktdev.mediaprocessing.coordinator.listeners.events

import mu.KotlinLogging
import no.iktdev.eventi.events.SingleTaskCreatorEventListener
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.SingleTaskCratedEvent
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.CoverTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.SubtitleTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.VideoTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreMediaInfoAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.requireEvent
import no.iktdev.mediaprocessing.shared.common.requireQualifiedEntry
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.springframework.stereotype.Component
import java.io.File

@Component
class StoreMetadataAndReferencesListener: SingleTaskCreatorEventListener(eventStore = EventStore, taskStore = TaskStore) {
    val log = KotlinLogging.logger {}

    override fun isEventOfMyCreation(event: Event) = event is StoreMediaInfoAndMetadataTaskCreatedEvent

    override fun onCreateTask(
        event: Event,
        history: List<Event>
    ): Task? {
        val startEvent = history.requireEvent<StartProcessingEvent>()

        val useEvent = history.getInstanceOf<ContinuationSummaryEvent>() ?: return null
        history.requireEvent<PersistContentEvent>()


        val data = if (startEvent.data.operation.isOnly(OperationType.MetadataSearch)) {
            event.requireQualifiedEntry<PersistContentEvent>()
            useEvent.data

        } else {
            val inEvent = event.requireQualifiedEntry<TransferredContentsSummaryEvent>()
            getStorageContent(useEvent.data, inEvent, history)
        }

        return StoreMediaInfoAndMetadataTask(data)
    }

    fun getStorageContent(data: ContentExport, transferred: TransferredContentsSummaryEvent, history: List<Event>): ContentExport {
        val usableEvents = history.filter { it.eventId in transferred.summarizedEventIds }

        val copyCoverEvent = usableEvents.getInstanceOf<CoverTransferredResultEvent>()
            .takeIf { it?.status == TaskStatus.Completed }

        val coverFileName = copyCoverEvent?.fileUri?.let { uri ->
            if (uri.isNotBlank()) File(uri).name else null
        }

        val metadata = data.metadata?.copy(
            cover = coverFileName
        )

        val videoCopy = usableEvents.getInstanceOf<VideoTransferredResultEvent>()
            ?.takeIf { it.status == TaskStatus.Completed }

        val subtitles = usableEvents.getInstancesOf<SubtitleTransferredResultEvent>()
            .filter { it.status == TaskStatus.Completed }
            .mapNotNull { it ->
                it.fileUri?.takeIf { uri -> uri.isNotBlank() }?.let { uri ->
                    ContentExport.MediaExport.Subtitle(File(uri).name, it.language)
                }
            }

        val export = ContentExport.MediaExport(
            videoFile = videoCopy?.fileUri?.let { File(it).name },
            subtitles = subtitles,
        )

        return ContentExport(
            collection = data.collection,
            metadata = metadata,
            episodeInfo = if (videoCopy != null) data.episodeInfo else null,
            media = export
        )
    }



    override fun onTaskCreated(
        event: Event,
        history: List<Event>,
        task: Task
    ): SingleTaskCratedEvent {

        return StoreMediaInfoAndMetadataTaskCreatedEvent(task.taskId)
            .derivedOf(event)
    }
}