package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecConfig

data class VideoPreference(
    val codec: VideoCodecConfig,
    val enforceMkv: Boolean = false
)