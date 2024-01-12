package no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

// Derived from ffmpeg work
@KafkaBelongsToEvent(
    KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED
)
data class ProcesserExtractWorkPerformed(
    override val status: Status,
    override val message: String? = null,
    val producedBy: String,
    val derivedFromEventId: String,
    val outFile: String? = null
): MessageDataWrapper(status, message)