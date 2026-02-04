package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference

import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecType
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType

data class ProcesserPreference(
    val videoPreference: VideoPreference? = null,
    val audioPreference: AudioPreference? = null
) {
    companion object {
        fun default(): ProcesserPreference {
            return ProcesserPreference(
                videoPreference = VideoPreference(VideoCodecConfig.from(VideoCodecType.HEVC), false),
                audioPreference = AudioPreference(
                    default = AudioCodecConfig.from(AudioCodecType.AAC)
                ),
            )
        }
    }
}