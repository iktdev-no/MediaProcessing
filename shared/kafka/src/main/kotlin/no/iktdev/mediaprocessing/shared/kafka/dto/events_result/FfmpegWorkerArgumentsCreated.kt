package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.streamit.library.kafka.dto.Status

/**
 * @param status Status type
 * @param inputFile File.absolutePath
 * @param outputFile File.absolutePath
 * @param arguments Requires arguments, instructions for what ffmpeg should do
 */
@KafkaBelongsToEvent(
    KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED,
    KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED
)
data class FfmpegWorkerArgumentsCreated(
    override val status: Status,
    val inputFile: String, // absolutePath
    val entries: List<FfmpegWorkerArgument>
):
        MessageDataWrapper(status)

data class FfmpegWorkerArgument(
    val outputFile: String,
    val arguments: List<String>
)