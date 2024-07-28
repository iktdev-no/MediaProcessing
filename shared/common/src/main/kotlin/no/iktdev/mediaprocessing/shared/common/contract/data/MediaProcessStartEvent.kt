package no.iktdev.mediaprocessing.shared.common.contract.data

import no.iktdev.eventi.data.EventMetadata
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.ProcessType
import no.iktdev.mediaprocessing.shared.common.contract.dto.StartOperationEvents

data class MediaProcessStartEvent(
    override val metadata: EventMetadata,
    override val data: StartEventData?,
    override val eventType: Events = Events.EventMediaProcessStarted
): Event()

data class StartEventData(
    val type: ProcessType = ProcessType.FLOW,
    val operations: List<StartOperationEvents> = listOf(
        StartOperationEvents.ENCODE,
        StartOperationEvents.EXTRACT,
        StartOperationEvents.CONVERT
    ),
    val file: String // AbsolutePath
)