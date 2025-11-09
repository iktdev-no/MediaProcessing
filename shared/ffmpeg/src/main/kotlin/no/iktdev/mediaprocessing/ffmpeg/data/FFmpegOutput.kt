package no.iktdev.mediaprocessing.ffmpeg.data

data class FFmpegOutput(
    override val success: Boolean
) : FFOutput() {
}