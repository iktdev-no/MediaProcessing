package no.iktdev.mediaprocessing.shared.kafka.dto.events_result

import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaBelongsToEvent
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status

@KafkaBelongsToEvent(KafkaEvents.EventMediaParseStreamPerformed)
data class MediaStreamsParsePerformed(
    override val status: Status,
    val streams: ParsedMediaStreams,
    override val derivedFromEventId: String?
) : MessageDataWrapper(status, derivedFromEventId)