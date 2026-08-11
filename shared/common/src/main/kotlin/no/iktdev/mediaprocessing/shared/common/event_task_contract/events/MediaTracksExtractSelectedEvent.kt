package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event

data class MediaTracksExtractSelectedEvent(
    val selectedSubtitleTracks: List<Int>,
    val excludedTracks: List<MediaTracksExtractExcluded>? = emptyList(),
): Event() {

}

data class MediaTracksExtractExcluded(
    val trackId: Int,
    val reason: String
)