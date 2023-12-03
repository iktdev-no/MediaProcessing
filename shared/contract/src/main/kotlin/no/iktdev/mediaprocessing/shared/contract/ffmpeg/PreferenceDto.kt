package no.iktdev.mediaprocessing.shared.contract.ffmpeg

data class PreferenceDto(
    val encodePreference: EncodingPreference = EncodingPreference(video = VideoPreference(), audio = AudioPreference()),
    val convertPreference: ConvertPreference = ConvertPreference()
)


data class EncodingPreference(
    val video: VideoPreference,
    val audio: AudioPreference
)

enum class SubtitleTypes {
    SRT,
    VTT,
    SMI
}

data class ConvertPreference(
    val cleanup: Boolean = true,
    val merge: Boolean = false,
    val subtitleTypes: List<SubtitleTypes> = listOf(
        SubtitleTypes.SRT, SubtitleTypes.VTT, SubtitleTypes.SMI
    )
)

data class VideoPreference(
    val codec: String = "h264",
    val pixelFormat: String = "yuv420p",
    val pixelFormatPassthrough: List<String> = listOf<String>("yuv420p", "yuv420p10le"),
    val threshold: Int = 16
)

data class AudioPreference(
    val codec: String = "aac",
    val sample_rate: Int? = null,
    val channels: Int? = null,
    val language: String = "eng", //ISO3 format
    val preserveChannels: Boolean = true,
    val defaultToEAC3OnSurroundDetected: Boolean = true,
    val forceStereo: Boolean = false
)
