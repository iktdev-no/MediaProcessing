package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_WORK_CONVERT_PERFORMED)
data class ConvertWorkPerformed(
    override val status: Status,
    override val message: String? = null,
    val producedBy: String,
    val derivedFromEventId: String,
    val result: List<String>
): MessageDataWrapper(status, message)