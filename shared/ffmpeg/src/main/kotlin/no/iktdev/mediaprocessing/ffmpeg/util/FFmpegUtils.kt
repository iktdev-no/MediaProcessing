package no.iktdev.mediaprocessing.ffmpeg.util

enum class FfmpegCodecs(val ffmpegName: String) {
    hevc("libx265"),
    h264("libx264"),
    vp9("libvpx-vp9"),
    av1("libaom-av1"),
    vid("libxvid"),
    vvc("libvvc"),
    vp8("libvpx");

    fun getCodecs(): List<FfmpegCodecs> {
        return entries
    }
}


fun CodecNameToFfmpegCodec(name: String): FfmpegCodecs {
    return when (name.lowercase()) {
        "hevc", "hevec", "h265", "h.265", "libx265" -> FfmpegCodecs.hevc
        "h.264", "h264", "libx264" -> FfmpegCodecs.h264
        "vp9", "vp-9", "libvpx-vp9" -> FfmpegCodecs.vp9
        "av1", "libaom-av1" -> FfmpegCodecs.av1
        "mpeg4", "mp4", "libxvid" -> FfmpegCodecs.vid
        "vvc", "h.266", "libvvc" -> FfmpegCodecs.vvc
        "vp8", "libvpx" -> FfmpegCodecs.vp8
        else -> throw IllegalArgumentException("Unsupported codec: $name")
    }
}

