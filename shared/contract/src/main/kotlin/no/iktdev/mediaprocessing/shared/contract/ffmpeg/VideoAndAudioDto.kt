package no.iktdev.mediaprocessing.shared.contract.ffmpeg


data class VideoAndAudioDto(
    val video: VideoArgumentsDto,
    val audio: AudioArgumentsDto,
    val outFile: String // Absolute path to file
)