package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio

data class AudioPreference(
    val default: AudioCodecConfig,
    val extended: AudioCodecConfig? = null
)
