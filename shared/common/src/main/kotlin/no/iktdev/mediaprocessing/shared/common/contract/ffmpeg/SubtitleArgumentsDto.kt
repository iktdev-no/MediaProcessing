package no.iktdev.mediaprocessing.shared.common.contract.ffmpeg

data class SubtitleArgumentsDto(
    val index: Int,
    val language: String,
    val format: String, // Extension as well
    val codecParameters: List<String> = listOf("-c:s", "copy"),
    val optionalParameters: List<String> = listOf()
)