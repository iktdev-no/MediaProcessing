package no.iktdev.mediaprocessing.shared.common.contract.ffmpeg

data class AudioArgumentsDto(
    val index: Int,
    val codecParameters: List<String> = listOf("-acodec", "copy"),
    val optionalParameters: List<String> = listOf()
)