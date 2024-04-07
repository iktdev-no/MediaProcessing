package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventMediaReadOutCover)
data class CoverInfoPerformed(
    override val status: Status,
    val url: String,
    val outDir: String,
    val outFileBaseName: String,
    override val derivedFromEventId: String?
) : MessageDataWrapper(status, derivedFromEventId)