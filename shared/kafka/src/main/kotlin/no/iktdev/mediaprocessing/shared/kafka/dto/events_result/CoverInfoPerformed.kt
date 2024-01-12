package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_MEDIA_READ_OUT_COVER)
data class CoverInfoPerformed(
    override val status: Status,
    val url: String,
    val outDir: String,
    val outFileBaseName: String
)
    : MessageDataWrapper(status)