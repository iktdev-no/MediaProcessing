package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_WORK_DOWNLOAD_COVER_PERFORMED)
data class CoverDownloadWorkPerformed(
    override val status: Status,
    override val message: String? = null,
    val coverFile: String
): MessageDataWrapper(status, message)
