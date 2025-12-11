package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams

data class MediaStreamParsedEvent(
    val data: ParsedMediaStreams
): Event() {
}
