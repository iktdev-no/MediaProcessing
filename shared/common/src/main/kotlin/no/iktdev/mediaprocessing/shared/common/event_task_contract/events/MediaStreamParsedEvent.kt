package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.model.ParsedMediaStreams

data class MediaStreamParsedEvent(
    val data: ParsedMediaStreams
): Event() {
}
