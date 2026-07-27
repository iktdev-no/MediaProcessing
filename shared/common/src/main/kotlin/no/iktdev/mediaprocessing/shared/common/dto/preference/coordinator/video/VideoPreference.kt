package no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video

data class VideoPreference(
    val codec: VideoCodecConfig,
    val enforceMkv: Boolean = false
)