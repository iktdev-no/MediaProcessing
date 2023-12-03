package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_PROCESS_STARTED)
data class ProcessStarted(
    override val status: Status,
    val type: ProcessType = ProcessType.FLOW,
    val file: String // AbsolutePath
) : MessageDataWrapper(status)