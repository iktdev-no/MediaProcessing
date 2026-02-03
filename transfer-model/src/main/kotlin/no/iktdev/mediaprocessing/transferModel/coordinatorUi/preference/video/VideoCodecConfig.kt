package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video

data class VideoCodecConfig(
    val type: VideoCodecType,
    val crf: Int? = null,
    val bitrate: Int? = null,
    val preset: Presets? = null,
    val profile: H264Profiles? = null,
    val level: Double? = null,
    val tune: String? = null,
    val cpuUsed: Int? = null,
    val qscale: Int? = null,
    val compressionLevel: Int? = null
) {
    companion object {
        fun from(codecType: VideoCodecType): VideoCodecConfig =
            when (codecType) {

                VideoCodecType.HEVC -> VideoCodecConfig(
                    type = codecType,
                    crf = 18,
                    preset = Presets.Slow,
                    tune = null,
                    bitrate = null
                )

                VideoCodecType.H264 -> VideoCodecConfig(
                    type = codecType,
                    crf = 23,
                    preset = Presets.Slow,
                    profile = H264Profiles.High,
                    level = 4.2,
                    bitrate = null
                )

                VideoCodecType.VP9 -> VideoCodecConfig(
                    type = codecType,
                    crf = 32,
                    cpuUsed = 4,
                    bitrate = null
                )

                VideoCodecType.VP8 -> VideoCodecConfig(
                    type = codecType,
                    crf = 10,
                    bitrate = null
                )

                VideoCodecType.AV1 -> VideoCodecConfig(
                    type = codecType,
                    crf = 30,
                    cpuUsed = 4
                )

                VideoCodecType.VVC -> VideoCodecConfig(
                    type = codecType,
                    crf = 27,
                    preset = Presets.Medium,
                    bitrate = null
                )

                VideoCodecType.XVID -> VideoCodecConfig(
                    type = codecType,
                    bitrate = null,
                    qscale = null
                )

                VideoCodecType.RAW -> VideoCodecConfig(
                    type = codecType
                )

                VideoCodecType.COPY -> VideoCodecConfig(
                    type = codecType
                )
            }
    }
}


enum class VideoCodecType {
    HEVC, H264, VP9, VP8, AV1, VVC, XVID, RAW, COPY
}

