package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.video

data class VideoPreference(
    val codec: VideoCodecConfig,
    val enforceMkv: Boolean = false
)