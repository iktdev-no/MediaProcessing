package no.iktdev.mediaprocessing.shared.contract.ffmpeg

data class AudioArgumentsDto(
    val index: Int,
    val codecParameters: List<String> = listOf("-acodec", "copy"),
    val optionalParameters: List<String> = listOf()

)