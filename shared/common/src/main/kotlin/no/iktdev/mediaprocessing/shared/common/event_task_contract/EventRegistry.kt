package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*

object EventRegistry {
    fun getEvents(): List<Class<out Event>> {
        return listOf(
            CollectedEvent::class.java,
            CompletedEvent::class.java,

            ConvertTaskCreatedEvent::class.java,
            ConvertTaskResultEvent::class.java,

            CoordinatorReadStreamsResultEvent::class.java,
            CoordinatorReadStreamsTaskCreatedEvent::class.java,

            CoverDownloadTaskCreatedEvent::class.java,
            CoverDownloadResultEvent::class.java,

            DeleteSequenceEvent::class.java,
            DeletedTaskResultEvent::class.java,
            ForcedTaskResetAuditEvent::class.java,

            FileAddedEvent::class.java,
            FileReadyEvent::class.java,
            FileRemovedEvent::class.java,

            FilePrepareForWorkTaskCreatedEvent::class.java,
            FilePrepareForWorkResultEvent::class.java,

            ValidateFileAndMediaDataEvent::class.java,

            ManualAllowCompletionEvent::class.java,

            MediaParsedInfoEvent::class.java,
            MediaStreamParsedEvent::class.java,
            MediaTracksDetermineSubtitleTypeEvent::class.java,
            MediaTracksEncodeSelectedEvent::class.java,
            MediaTracksExtractSelectedEvent::class.java,

            MetadataSearchResultEvent::class.java,
            MetadataSearchTaskCreatedEvent::class.java,

            MigrateContentToStoreTaskCreatedEvent::class.java,
            MigrateContentToStoreTaskResultEvent::class.java,

            ProcesserEncodeResultEvent::class.java,
            ProcesserEncodeTaskCreatedEvent::class.java,

            ProcesserExtractResultEvent::class.java,
            ProcesserExtractTaskCreatedEvent::class.java,

            StartProcessingEvent::class.java,

            StoreContentAndMetadataTaskCreatedEvent::class.java,
            StoreContentAndMetadataTaskResultEvent::class.java
        )
    }
}