package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FileAddedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FileReadyEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FileRemovedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksDetermineSubtitleTypeEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksEncodeSelectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaTracksExtractSelectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodePerformedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractPerformedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskResultEvent

object EventRegistry {
    fun getEvents(): List<Class<out Event>> {
        return listOf(
            CollectedEvent::class.java,

            ConvertTaskCreatedEvent::class.java,
            ConvertTaskResultEvent::class.java,

            CoordinatorReadStreamsResultEvent::class.java,
            CoordinatorReadStreamsTaskCreatedEvent::class.java,

            CoverDownloadTaskCreatedEvent::class.java,
            CoverDownloadResultEvent::class.java,

            FileAddedEvent::class.java,
            FileReadyEvent::class.java,
            FileRemovedEvent::class.java,

            MediaParsedInfoEvent::class.java,
            MediaStreamParsedEvent::class.java,
            MediaTracksDetermineSubtitleTypeEvent::class.java,
            MediaTracksEncodeSelectedEvent::class.java,
            MediaTracksExtractSelectedEvent::class.java,

            MetadataSearchResultEvent::class.java,
            MetadataSearchTaskCreatedEvent::class.java,

            MigrateContentToStoreTaskCreatedEvent::class.java,
            MigrateContentToStoreTaskResultEvent::class.java,

            ProcesserEncodePerformedEvent::class.java,
            ProcesserEncodeResultEvent::class.java,
            ProcesserEncodeTaskCreatedEvent::class.java,

            ProcesserExtractPerformedEvent::class.java,
            ProcesserExtractResultEvent::class.java,
            ProcesserExtractTaskCreatedEvent::class.java,

            ProcesserEncodeTaskCreatedEvent::class.java,
            ProcesserEncodeResultEvent::class.java,

            StartProcessingEvent::class.java,

            StoreContentAndMetadataTaskCreatedEvent::class.java,
            StoreContentAndMetadataTaskResultEvent::class.java
        )
    }
}