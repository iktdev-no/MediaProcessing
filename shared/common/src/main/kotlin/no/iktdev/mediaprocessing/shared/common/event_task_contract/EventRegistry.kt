package no.iktdev.mediaprocessing.shared.common.event_task_contract

import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FileAddedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FileReadyEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FileRemovedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchTaskCreated
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchTaskPerformed
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserReadTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskCreatedEvent

object EventRegistry {
    fun getEvents(): List<Class<out Event>> {
        return listOf(
            ConvertTaskCreatedEvent::class.java,
            ConvertTaskResultEvent::class.java,

            FileAddedEvent::class.java,
            FileReadyEvent::class.java,
            FileRemovedEvent::class.java,

            MediaParsedInfoEvent::class.java,

            MetadataSearchTaskCreated::class.java,
            MetadataSearchTaskPerformed::class.java,

            ProcesserExtractTaskCreatedEvent::class.java,
            ProcesserExtractEvent::class.java,

            ProcesserEncodeTaskCreatedEvent::class.java,
            ProcesserEncodeEvent::class.java,

            ProcesserReadTaskCreatedEvent::class.java, // Do i need this?

            StartProcessingEvent::class.java
        )
    }
}