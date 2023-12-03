package no.iktdev.mediaprocessing.shared.contract.ffmpeg

data class VideoArgumentsDto(
    val index: Int,
    val codecParameters: List<String> = listOf("-vcodec", "copy"),
    val optionalParameters: List<String> = listOf()
)