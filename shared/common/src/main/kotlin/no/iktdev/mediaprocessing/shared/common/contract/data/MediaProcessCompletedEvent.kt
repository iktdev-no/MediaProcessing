package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events

data class MediaProcessCompletedEvent(
    override val metadata: EventMetadata,
    override val data: CompletedEventData?,
    override val eventType: Events = Events.EventMediaProcessCompleted
): Event()

data class CompletedEventData(
    val eventIdsCollected: List<String>
)