package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.dto.ProcessStartOperationEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_MEDIA_PROCESS_STARTED)
data class MediaProcessStarted(
    override val status: Status,
    val type: ProcessType = ProcessType.FLOW,
    val operations: List<ProcessStartOperationEvents> = listOf(
        ProcessStartOperationEvents.ENCODE,
        ProcessStartOperationEvents.EXTRACT,
        ProcessStartOperationEvents.CONVERT
    ),
    val file: String // AbsolutePath
) : MessageDataWrapper(status)