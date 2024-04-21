package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventMediaReadBaseInfoPerformed)
data class BaseInfoPerformed(
    override val status: Status,
    val title: String,
    val sanitizedName: String,
    val searchTitles: List<String> = emptyList<String>(),
    override val derivedFromEventId: String
) : MessageDataWrapper(status = status, derivedFromEventId = derivedFromEventId)

fun BaseInfoPerformed?.hasValidData(): Boolean {
    return this != null && this.title.isNotBlank() && this.sanitizedName.isNotBlank()
}