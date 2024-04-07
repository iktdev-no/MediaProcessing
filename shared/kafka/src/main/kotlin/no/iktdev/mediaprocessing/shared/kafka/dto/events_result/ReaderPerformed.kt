package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import com.google.gson.JsonObject
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventMediaReadStreamPerformed)
data class ReaderPerformed(
    override val status: Status,
    val file: String, //AbsolutePath
    val output: JsonObject,
    override val derivedFromEventId: String?
) : MessageDataWrapper(status, derivedFromEventId)