package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EVENT_WORK_CONVERT_CREATED)
data class ConvertWorkerRequest(
    override val status: Status,
    val requiresEventId: String? = null,
    val inputFile: String,
    val allowOverwrite: Boolean,
    val outFileBaseName: String,
    val outDirectory: String
): MessageDataWrapper(status)