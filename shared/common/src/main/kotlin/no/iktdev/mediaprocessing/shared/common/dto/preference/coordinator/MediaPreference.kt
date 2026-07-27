package no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator

import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecConfig
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioCodecType
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio.AudioPreference
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoCodecConfig
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoCodecType
import no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.video.VideoPreference

data class MediaPreference(
    val videoPreference: VideoPreference? = null,
    val audioPreference: AudioPreference? = null,
) {
    companion object {
        fun default(): MediaPreference {
            return MediaPreference(
                videoPreference = VideoPreference(VideoCodecConfig.from(VideoCodecType.HEVC), false),
                audioPreference = AudioPreference(
                    default = AudioCodecConfig.from(AudioCodecType.AAC)
                        .copy(channels = 2, bitrate = 128, sampleRate = 48000)
                ),
            )
        }
    }
}