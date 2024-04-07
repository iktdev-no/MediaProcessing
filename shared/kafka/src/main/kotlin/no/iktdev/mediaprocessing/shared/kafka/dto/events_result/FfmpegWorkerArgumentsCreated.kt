package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

/**
 * @param status Status type
 * @param inputFile File.absolutePath
 * @param outputFile File.absolutePath
 * @param arguments Requires arguments, instructions for what ffmpeg should do
 */
@KafkaBelongsToEvent(
    KafkaEvents.EventMediaParameterEncodeCreated,
    KafkaEvents.EventMediaParameterExtractCreated
)
data class FfmpegWorkerArgumentsCreated(
    override val status: Status,
    val inputFile: String, // absolutePath
    val entries: List<FfmpegWorkerArgument>,
    override val derivedFromEventId: String?
) : MessageDataWrapper(status, derivedFromEventId)

data class FfmpegWorkerArgument(
    val outputFile: String,
    val arguments: List<String>
)