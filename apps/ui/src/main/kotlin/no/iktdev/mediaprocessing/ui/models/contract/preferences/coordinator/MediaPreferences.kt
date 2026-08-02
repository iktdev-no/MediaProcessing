package no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator

data class MediaPreference(
    val videoPreference: VideoPreference? = null,
    val audioPreference: AudioPreference? = null,
)

data class VideoPreference(
    val codec: VideoCodecConfig,
    val enforceMkv: Boolean = false
)

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
)

enum class VideoCodecType {
    HEVC, H264, VP9, VP8, AV1, VVC, XVID, RAW, COPY
}

enum class Presets(val presetName: String) {
    Ultrafast("ultrafast"),
    Superfast("superfast"),
    Veryfast("veryfast"),
    Faster("faster"),
    Fast("fast"),
    Medium("medium"),
    Slow("slow"),
    Slower("slower"),
    Veryslow("veryslow"),
    Placebo("placebo")
}

enum class H264Profiles(val profileName: String) {
    Baseline("baseline"),
    Main("main"),
    High("high"),
    High10("high10"),
    High422("high422"),
    High444("high444")
}

//#region Audio Preference
data class AudioPreference(
    val default: AudioCodecConfig,
    val extended: AudioCodecConfig? = null
)

data class AudioCodecConfig(
    val type: AudioCodecType,
    val bitrate: Int? = null,
    val sampleRate: Int? = null,
    val channels: Int? = null,
    val profile: AacProfile? = null,
    val application: OpusApplication? = null,
    val compressionLevel: Int? = null
)

enum class AudioCodecType {
    AAC, MP3, OPUS, VORBIS, FLAC, AC3, EAC3, DTS, PCM, COPY
}

enum class AacProfile(val ffmpegName: String) {
    LC("aac_low"),   // Low Complexity – mest brukt
    HE("aac_he"),    // High Efficiency – bedre komprimering
    HEv2("aac_he_v2") // High Efficiency v2 – enda mer komprimering
}

enum class OpusApplication(val ffmpegName: String) {
    Audio("audio"),      // Vanlig musikk/lyd
    Voip("voip"),        // Optimalisert for tale
    LowDelay("lowdelay") // Lav latency, f.eks. live streaming
}

//#endregion