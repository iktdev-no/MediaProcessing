package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event

data class MediaTracksEncodeSelectedEvent(
    val selectedVideoTrack: Int,
    val selectedAudioTrack: Int,
    val selectedAudioExtendedTrack: Int? = null // Optional extended audio track, e.g Dolby Atmos or Enhanced AAC
): Event()