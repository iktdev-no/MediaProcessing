package no.iktdev.mediaprocessing.shared.common.rules

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.mediaprocessing.shared.common.UtcNow
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.TransferContentTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.getName
import java.time.Duration

object EventLifecycleRules {

    fun isOverdue(events: List<PersistedEvent>): Boolean {
        if (events.isEmpty()) return false

        val firstEventTime = events.minOf { it.persistedAt }
        val expectedWindow = expectedCompletionTimeWindow(events)

        val deadline = firstEventTime.plus(expectedWindow)

        return UtcNow().isAfter(deadline)
    }

    fun expectedCompletionTimeWindow(events: List<PersistedEvent>): Duration =
        events.fold(Duration.ZERO) { acc, pe ->
            acc +
                    pe.match<CoordinatorReadStreamsTaskCreatedEvent>(Duration.ofMinutes(5)) +
                    pe.match<ConvertTaskCreatedEvent>(Duration.ofMinutes(5)) +
                    pe.match<CoverDownloadTaskCreatedEvent>(Duration.ofMinutes(5)) +
                    pe.match<MetadataSearchTaskCreatedEvent>(Duration.ofMinutes(10)) +
                    pe.match<TransferContentTaskCreatedEvent>(Duration.ofMinutes(5)) +
                    pe.match<ProcesserEncodeTaskCreatedEvent>(Duration.ofHours(8)) +
                    pe.match<ProcesserExtractTaskCreatedEvent>(Duration.ofMinutes(15)) +
                    pe.match<StoreMediaInfoAndMetadataTaskCreatedEvent>(Duration.ofMinutes(5))
        }

    inline fun <reified T : Event> PersistedEvent.match(duration: Duration): Duration =
        if (this.event == T::class.getName()) duration else Duration.ZERO
}
