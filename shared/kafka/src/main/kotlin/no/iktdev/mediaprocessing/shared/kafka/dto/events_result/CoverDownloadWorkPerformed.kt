package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventWorkDownloadCoverPerformed)
data class CoverDownloadWorkPerformed(
    override val status: Status,
    override val message: String? = null,
    val coverFile: String,
    override val derivedFromEventId: String?
): MessageDataWrapper(status, message, derivedFromEventId = derivedFromEventId)
