package no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio

data class AudioPreference(
    val default: AudioCodecConfig,
    val extended: AudioCodecConfig? = null
)
