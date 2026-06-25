package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.delete.DeleteSequenceEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.delete.DeletedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.delete.DeletedTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.CoverTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.SubtitleTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.TransferContentTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.VideoTransferredResultEvent

object EventRegistry {
    fun getEvents(): List<Class<out Event>> {
        return listOf(
            AlterOverrideEvent::class.java,
            AlteredOverrideEvent::class.java,

            CollectedEvent::class.java,
            CompletedEvent::class.java,
            CompletedCacheDeletedEvent::class.java,
            CompletedInputDeletedEvent::class.java,
            ContinuationSummaryEvent::class.java,

            ConvertTaskCreatedEvent::class.java,
            ConvertTaskResultEvent::class.java,

            CoordinatorReadStreamsResultEvent::class.java,
            CoordinatorReadStreamsTaskCreatedEvent::class.java,

            CoverDownloadTaskCreatedEvent::class.java,
            CoverDownloadResultEvent::class.java,
            CoverDownloadSkippedEvent::class.java,

            DeleteSequenceEvent::class.java,
            DeletedEvent::class.java,
            DeletedTaskResultEvent::class.java,
            ForcedTaskResetAuditEvent::class.java,

            FileAddedEvent::class.java,
            FileReadyEvent::class.java,
            FileChangedEvent::class.java,
            FileRemovedEvent::class.java,

            FilePrepareForWorkTaskCreatedEvent::class.java,
            FilePrepareForWorkResultEvent::class.java,

            ValidateFileAndMediaDataEvent::class.java,


            MediaParsedInfoEvent::class.java,
            MediaStreamParsedEvent::class.java,
            MediaTracksDetermineSubtitleTypeEvent::class.java,
            MediaTracksEncodeSelectedEvent::class.java,
            MediaTracksExtractSelectedEvent::class.java,

            MetadataSearchResultEvent::class.java,
            MetadataSearchTaskCreatedEvent::class.java,

            DetermineCollectionTaskCreatedEvent::class.java,
            DeterminedCollectionTaskResultEvent::class.java,

            TransferContentTaskCreatedEvent::class.java,
            VideoTransferredResultEvent::class.java,
            CoverTransferredResultEvent::class.java,
            SubtitleTransferredResultEvent::class.java,

            ProcesserEncodeResultEvent::class.java,
            ProcesserEncodeTaskCreatedEvent::class.java,

            ProcesserExtractResultEvent::class.java,
            ProcesserExtractTaskCreatedEvent::class.java,

            PersistContentEvent::class.java,
            OnHoldSignalEvent::class.java,
            ReleaseHoldSignalEvent::class.java,

            StartProcessingEvent::class.java,

            StoreMediaInfoAndMetadataTaskCreatedEvent::class.java,
            StoreMediaInfoAndMetadataTaskResultEvent::class.java
        )
    }
}