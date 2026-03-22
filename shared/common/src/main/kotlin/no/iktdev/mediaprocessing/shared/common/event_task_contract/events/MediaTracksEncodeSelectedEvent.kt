package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event

data class MediaTracksEncodeSelectedEvent(
    val selectedVideoTrack: Int,
    val audioTracks: List<SelectedAudioTracks>
): Event() {
    data class SelectedAudioTracks(
        val language: String,
        val defaultListIndex: Int,
        val defaultFfmpegIndex: Int,
        val extendedListIndex: Int?,
        val extendedFfmpegIndex: Int?
    )
}