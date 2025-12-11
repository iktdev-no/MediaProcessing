package no.iktdev.mediaprocessing.ffmpeg.dsl

import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream

sealed class VideoCodec(val codec: String, open val crf: Int? = null, open val bitrate: Int? = null) {

    // HEVC / H.265 encoder (libx265)
    class Hevc(
        // Preset styrer hastighet vs komprimeringseffektivitet.
        // "ultrafast" = rask, men stor fil; "veryslow" = treg, men liten fil.
        var preset: Presets = Presets.Slow,

        // CRF (Constant Rate Factor) styrer kvalitet vs bitrate.
        // Lavere tall = bedre kvalitet, høyere tall = lavere bitrate.
        override var crf: Int = 18,
        override val bitrate: Int? = null,

        // Tune kan brukes for spesifikke scenarier (film, animation, grain).
        var tune: String? = null,
    ) : VideoCodec("libx265") {

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
        override fun buildFfmpegArgs(stream: VideoStream): List<String> {
            val args = super.buildFfmpegArgs(stream).toMutableList()
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
        override var crf: Int = 23,
        override val bitrate: Int? = null
    ) : VideoCodec("libx264") {
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
        override fun buildFfmpegArgs(stream: VideoStream): List<String> {
            val args = super.buildFfmpegArgs(stream).toMutableList()
            args += listOf("-preset", preset.presetName)
            args += listOf("-profile:v", profile.profileName)
            args += listOf("-level", level.toString())
            return args
        }
    }


    // VP9 encoder (libvpx-vp9)
    class Vp9(
        // CRF: styrer kvalitet vs bitrate for VP9.
        override var crf: Int = 32,

        // Bitrate: kan settes eksplisitt i kbps hvis du vil ha CBR/VBR.
        override var bitrate: Int? = null,

        var cpuUsed: Int = 4 // Tradeoff mellom hastighet og komprimering.
    ) : VideoCodec("libvpx-vp9") {
        override fun determineTranscodeDecision(stream: VideoStream): TranscodeDecision {
            val superDecision = super.determineTranscodeDecision(stream)
            if (superDecision == TranscodeDecision.Reencode) return superDecision

            // MP4 har dårlig VP9-støtte, WebM er tryggere
            val containerOk = stream.codec_tag_string.equals("vp09", ignoreCase = true)

            return if (containerOk) TranscodeDecision.Copy else TranscodeDecision.Remux
        }
        override fun buildFfmpegArgs(stream: VideoStream): List<String> {
            val args = super.buildFfmpegArgs(stream).toMutableList()
            args += listOf("-cpu-used", cpuUsed.toString())
            return args
        }
    }


    // VP8 encoder (libvpx)
    class Vp8(
        override var crf: Int = 10,
        override var bitrate: Int? = null
    ) : VideoCodec("libvpx")


    // AV1 encoder (libaom-av1)
    class Av1(
        // CRF: styrer kvalitet vs bitrate for AV1.
        override var crf: Int = 30,

        // cpuUsed: tradeoff mellom hastighet og komprimering.
        // Lav verdi = treg, men effektiv; høy verdi = rask, men mindre effektiv.
        var cpuUsed: Int = 4
    ) : VideoCodec("libaom-av1") {
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
        override fun buildFfmpegArgs(stream: VideoStream): List<String> {
            val args = super.buildFfmpegArgs(stream).toMutableList()
            args += listOf("-cpu-used", cpuUsed.toString())
            return args
        }
    }


    // VVC (Versatile Video Coding, H.266)
    class Vvc(
        // Preset: encoding speed vs compression tradeoff.
        var preset: Presets = Presets.Medium,
        override var crf: Int = 27,
        override val bitrate: Int? = null
    ) : VideoCodec("libvvc") {
        override fun buildFfmpegArgs(stream: VideoStream): List<String> {
            val args = super.buildFfmpegArgs(stream).toMutableList()
            args += listOf("-preset", preset.presetName)
            return args
        }
    }


    // Xvid (MPEG-4 Part 2)
    class Vid(
        // Bitrate: typisk parameter for Xvid, ofte brukt i kbps.
        override var bitrate: Int? = null,
        var qscale: Int? = null // Xvid bruker qscale i stedet for CRF
    ) : VideoCodec("libxvid") {
        override fun buildFfmpegArgs(stream: VideoStream): List<String> {
            val args = mutableListOf("-c:v", codec)
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
        return if (this.isSame(stream.codec_name)) {
            TranscodeDecision.Copy
        } else {
            when (this) {
                is Copy -> TranscodeDecision.Copy
                else -> TranscodeDecision.Reencode
            }
        }
    }

    open fun buildFfmpegArgs(stream: VideoStream): List<String> {
        val args = mutableListOf("-c:v", codec)

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