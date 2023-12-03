package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED)
data class BaseInfoPerformed(
    override val status: Status,
    val title: String,
    val sanitizedName: String
) : MessageDataWrapper(status)

fun BaseInfoPerformed?.hasValidData(): Boolean {
    return this != null && this.title.isNotBlank() && this.sanitizedName.isNotBlank()
}