package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.model.SubtitleItem

data class MediaTracksDetermineSubtitleTypeEvent(
    val subtitleTrackItems: List<SubtitleItem>
): Event() {

}