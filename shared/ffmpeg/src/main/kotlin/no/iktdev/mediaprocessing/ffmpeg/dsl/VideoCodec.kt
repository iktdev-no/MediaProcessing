package no.iktdev.mediaprocessing.ffmpeg.dsl

import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.mediaprocessing.ffmpeg.util.CodecNameToFfmpegCodec

open class VideoCodec(val codec: String, val crf: Int? = null, val bitrate: Int? = null) {

    // HEVC / H.265 encoder (libx265)
    class Hevc(
        // Preset styrer hastighet vs komprimeringseffektivitet.
        // "ultrafast" = rask, men stor fil; "veryslow" = treg, men liten fil.
        var preset: Presets = Presets.Slow,

        // CRF (Constant Rate Factor) styrer kvalitet vs bitrate.
        // Lavere tall = bedre kvalitet, høyere tall = lavere bitrate.
        crf: Int = 18,
        bitrate: Int? = null,

        // Tune kan brukes for spesifikke scenarier (film, animation, grain).
        var tune: String? = null,
    ) : VideoCodec(codec = "libx265", crf = crf, bitrate = bitrate) {

        override fun determineTranscodeDecision(stream: VideoStream): TranscodeDecision {
            val superDecision = super.determineTranscodeDecision(stream)
            if (superDecision == TranscodeDecision.Reencode) {
                return superDecision
            }

            val unsetTag = stream.codec_tag_string == "[0][0][0][0]" || stream.codec_tag == "0x0000"
            val validTag = stream.codec_tag_string.equals("hev1", ignoreCase = true) ||
                    stream.codec_tag_string.equals("hvc1", ignoreCase = true)

            val profileOk = stream.profile.lowercase() in listOf("main", "main10")
            val levelOk = stream.level <= 153 // 5.1 ≈ 153

            return when {
                // Profil eller level utenfor Chromecast‑krav → reencode
                !profileOk || !levelOk -> TranscodeDecision.Reencode

                // Tag unset eller gyldig → vi kan fortsatt copy/remux
                unsetTag || validTag -> TranscodeDecision.Copy

                // Alle andre tilfeller → safe fallback til reencode
                else -> TranscodeDecision.Reencode
            }
        }
        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val s = suffix ?: ""
            val args = super.buildFfmpegArgs(s).toMutableList()
            args += listOf("-preset", preset.presetName)
            tune?.let { args += listOf("-tune", it) }
            return args
        }

    }


    // H.264 encoder (libx264)
    class H264(
        // Preset: samme som for HEVC, styrer encoding speed vs compression.
        var preset: Presets = Presets.Slow,

        // Profile: Baseline/Main/High etc. styrer kompatibilitet og features.
        // High gir best kvalitet, Baseline brukes ofte for mobile enheter.
        var profile: H264Profiles = H264Profiles.High,

        // Level: definerer maks bitrate, oppløsning og framerate.
        // Eks: 4.1 passer for 1080p @ 30fps, 5.1 for 4K.
        var level: Double = 4.2,

        // CRF: styrer kvalitet vs bitrate (samme som for HEVC).
        crf: Int = 23,
        bitrate: Int? = null
    ) : VideoCodec(codec = "libx264", crf = crf, bitrate = bitrate) {
        override fun determineTranscodeDecision(stream: VideoStream): TranscodeDecision {
            val superDecision = super.determineTranscodeDecision(stream)
            if (superDecision == TranscodeDecision.Reencode) return superDecision

            val profileOk = stream.profile.lowercase() in listOf("baseline", "main", "high", "high10")
            val levelOk = stream.level <= 51 // 5.1 typisk maks for bred støtte

            return when {
                !profileOk || !levelOk -> TranscodeDecision.Reencode
                else -> TranscodeDecision.Copy
            }
        }
        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val s = suffix ?: ""
            val args = super.buildFfmpegArgs(s).toMutableList()
            args += listOf("-preset", preset.presetName)
            args += listOf("-profile:v", profile.profileName)
            args += listOf("-level", level.toString())
            return args
        }

    }


    // VP9 encoder (libvpx-vp9)
    class Vp9(
        // CRF: styrer kvalitet vs bitrate for VP9.
        crf: Int = 32,

        // Bitrate: kan settes eksplisitt i kbps hvis du vil ha CBR/VBR.
        bitrate: Int? = null,

        var cpuUsed: Int = 4 // Tradeoff mellom hastighet og komprimering.
    ) : VideoCodec(codec = "libvpx-vp9", crf = crf, bitrate = bitrate) {
        override fun determineTranscodeDecision(stream: VideoStream): TranscodeDecision {
            val superDecision = super.determineTranscodeDecision(stream)
            if (superDecision == TranscodeDecision.Reencode) return superDecision

            // MP4 har dårlig VP9-støtte, WebM er tryggere
            val containerOk = stream.codec_tag_string.equals("vp09", ignoreCase = true)

            return if (containerOk) TranscodeDecision.Copy else TranscodeDecision.Remux
        }
        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val s = suffix ?: ""
            val args = super.buildFfmpegArgs(s).toMutableList()
            args += listOf("-cpu-used", cpuUsed.toString())
            return args
        }
    }


    // VP8 encoder (libvpx)
    class Vp8(
        crf: Int = 10,
        bitrate: Int? = null
    ) : VideoCodec(codec = "libvpx", crf = crf, bitrate = bitrate)


    // AV1 encoder (libaom-av1)
    class Av1(
        // CRF: styrer kvalitet vs bitrate for AV1.
        crf: Int = 30,

        // cpuUsed: tradeoff mellom hastighet og komprimering.
        // Lav verdi = treg, men effektiv; høy verdi = rask, men mindre effektiv.
        var cpuUsed: Int = 4
    ) : VideoCodec(codec = "libaom-av1", crf = crf) {
        override fun determineTranscodeDecision(stream: VideoStream): TranscodeDecision {
            val superDecision = super.determineTranscodeDecision(stream)
            if (superDecision == TranscodeDecision.Reencode) return superDecision

            val validTag = stream.codec_tag_string.equals("av01", ignoreCase = true)
            val levelOk = stream.level <= 51 // AV1 nivåer, typisk ≤ 5.1

            return when {
                !validTag || !levelOk -> TranscodeDecision.Reencode
                else -> TranscodeDecision.Copy
            }
        }
        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val s = suffix ?: ""
            val args = super.buildFfmpegArgs(s).toMutableList()
            args += listOf("-cpu-used", cpuUsed.toString())
            return args
        }
    }


    // VVC (Versatile Video Coding, H.266)
    class Vvc(
        // Preset: encoding speed vs compression tradeoff.
        var preset: Presets = Presets.Medium,
        crf: Int = 27,
        bitrate: Int? = null
    ) : VideoCodec(codec = "libvvc", crf = crf, bitrate = bitrate) {
        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val s = suffix ?: ""
            val args = super.buildFfmpegArgs(s).toMutableList()
            args += listOf("-preset", preset.presetName)
            return args
        }
    }


    // Xvid (MPEG-4 Part 2)
    class Vid(
        // Bitrate: typisk parameter for Xvid, ofte brukt i kbps.
        bitrate: Int? = null,
        var qscale: Int? = null // Xvid bruker qscale i stedet for CRF
    ) : VideoCodec(codec = "libxvid", bitrate = bitrate) {
        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val s = suffix ?: ""
            val args = mutableListOf<String>()

            // enten qscale eller bitrate
            args += listOf("-c:v$s", codec)

            bitrate?.let { args += listOf("-b:v", "${it}k") }
            qscale?.let { args += listOf("-qscale:v", it.toString()) }

            return args
        }
    }

    // Raw video (ingen komprimering)
    object Raw : VideoCodec("rawvideo") {
        override fun determineTranscodeDecision(stream: VideoStream): TranscodeDecision {
            return TranscodeDecision.Reencode
        }
    }

    // Copy: ingen reenkoding, bare remuxing av eksisterende stream.
    object Copy : VideoCodec("copy")

    open fun determineTranscodeDecision(stream: VideoStream): TranscodeDecision {
        val ffmpegKnownCodec = CodecNameToFfmpegCodec(stream.codec_name)
        val isSameCodec = this.isSame(ffmpegKnownCodec.ffmpegName)
        if (isSameCodec) {
            return TranscodeDecision.Copy
        }
        val mode = when (this) {
            is Copy -> TranscodeDecision.Copy
            else -> TranscodeDecision.Reencode
        }
        return mode
    }

    open fun buildFfmpegArgs(suffix: String? = null): List<String> {
        val s = suffix ?: ""
        val args = mutableListOf("-c:v$s", codec)

        crf?.let { args += listOf("-crf", it.toString()) }
        bitrate?.let { args += listOf("-b:v", "${it}k") }

        return args
    }

}

fun VideoCodec.isSame(name: String): Boolean {
    val codecObject = when (name.lowercase()) {
        "hevc", "hevec", "h265", "h.265", "libx265" -> VideoCodec.Hevc()
        "h.264", "h264", "libx264" -> VideoCodec.H264()
        "vp9", "vp-9", "libvpx-vp9" -> VideoCodec.Vp9()
        "av1", "libaom-av1" -> VideoCodec.Av1()
        "mpeg4", "mp4", "libxvid" -> VideoCodec.Vid()
        "vvc", "h.266", "libvvc" -> VideoCodec.Vvc()
        "vp8", "libvpx" -> VideoCodec.Vp8()
        "rawvideo" -> VideoCodec.Raw
        "copy" -> VideoCodec.Copy
        else -> throw IllegalArgumentException("Unsupported codec: $name")
    }
    return (this.codec == codecObject.codec)
}

enum class H264Profiles(val profileName: String) {
    Baseline("baseline"),
    Main("main"),
    High("high"),
    High10("high10"),
    High422("high422"),
    High444("high444")
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