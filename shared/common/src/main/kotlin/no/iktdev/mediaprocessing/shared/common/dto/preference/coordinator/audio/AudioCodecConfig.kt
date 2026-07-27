package no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator.audio

data class AudioCodecConfig(
    val type: AudioCodecType,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val profile: AacProfile? = null,
    val application: OpusApplication? = null,
    val compressionLevel: Int? = null
) {
    companion object {
        fun from(codecType: AudioCodecType): AudioCodecConfig =
            when (codecType) {

                AudioCodecType.AAC -> AudioCodecConfig(
                    type = codecType,
                    bitrate = null,          // DSL default: no forced bitrate
                    sampleRate = null,       // DSL default: copy unless mismatch
                    channels = null,         // DSL default: copy unless mismatch
                    profile = AacProfile.LC  // DSL default
                )

                AudioCodecType.MP3 -> AudioCodecConfig(
                    type = codecType,
                    bitrate = null,
                    sampleRate = null,
                    channels = null
                )

                AudioCodecType.OPUS -> AudioCodecConfig(
                    type = codecType,
                    bitrate = null,
                    sampleRate = null,
                    channels = null,
                    application = OpusApplication.Audio // DSL default
                )

                AudioCodecType.VORBIS -> AudioCodecConfig(
                    type = codecType,
                    bitrate = null,
                    sampleRate = null,
                    channels = null
                )

                AudioCodecType.FLAC -> AudioCodecConfig(
                    type = codecType,
                    compressionLevel = null, // DSL default: no override
                    sampleRate = null,
                    channels = null
                )

                AudioCodecType.AC3 -> AudioCodecConfig(
                    type = codecType,
                    bitrate = null,
                    sampleRate = null,
                    channels = null
                )

                AudioCodecType.EAC3 -> AudioCodecConfig(
                    type = codecType,
                    bitrate = null,
                    sampleRate = null,
                    channels = null
                )

                AudioCodecType.DTS -> AudioCodecConfig(
                    type = codecType,
                    bitrate = null,
                    sampleRate = null,
                    channels = null
                )

                AudioCodecType.PCM -> AudioCodecConfig(
                    type = codecType,
                    sampleRate = null,
                    channels = null
                )

                AudioCodecType.COPY -> AudioCodecConfig(
                    type = codecType,
                    sampleRate = null,
                    channels = null,
                    bitrate = null
                )
            }
    }
}


enum class AudioCodecType {
    AAC, MP3, OPUS, VORBIS, FLAC, AC3, EAC3, DTS, PCM, COPY
}
