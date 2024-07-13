package no.iktdev.mediaprocessing.shared.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents

data class MediaProcessCompletedEvent(
    override val metadata: EventMetadata,
    override val data: CompletedEventData?,
    override val eventType: Events = Events.EventMediaProcessCompleted
): Event()

data class CompletedEventData(
    val eventIdsCollected: List<String>
)