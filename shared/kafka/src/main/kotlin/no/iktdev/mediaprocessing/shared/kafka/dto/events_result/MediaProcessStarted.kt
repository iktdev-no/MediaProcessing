package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventMediaProcessStarted)
data class MediaProcessStarted(
    override val status: Status,
    val type: ProcessType = ProcessType.FLOW,
    val operations: List<StartOperationEvents> = listOf(
        StartOperationEvents.ENCODE,
        StartOperationEvents.EXTRACT,
        StartOperationEvents.CONVERT
    ),
    val file: String // AbsolutePath
) : MessageDataWrapper(status)