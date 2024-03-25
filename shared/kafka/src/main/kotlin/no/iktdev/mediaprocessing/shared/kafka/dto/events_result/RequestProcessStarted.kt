package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.dto.ProcessStartOperationEvents
import no.iktdev.mediaprocessing.shared.contract.dto.RequestStartOperationEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_REQUEST_PROCESS_STARTED)
data class RequestProcessStarted(
    override val status: Status,
    val operations: List<RequestStartOperationEvents> = listOf(
        RequestStartOperationEvents.CONVERT
    ),
    val file: String // AbsolutePath
) : MessageDataWrapper(status)