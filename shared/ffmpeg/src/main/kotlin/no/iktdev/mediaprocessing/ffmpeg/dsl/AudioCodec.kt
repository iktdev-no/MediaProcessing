package no.iktdev.mediaprocessing.ffmpeg.dsl

import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.model.AudioClamp

sealed class AudioCodec(val codec: String, open var bitrate: Int? = null, open var sampleRate: Int? = null, open var channels: Int? = null) {

    // AAC (Advanced Audio Coding)
    class Aac(
        // Bitrate i kbps (typisk 128–256 for stereo)
        bitrate: Int? = null,
        // Profile: LC (Low Complexity), HE (High Efficiency), HEv2
        var profile: AacProfile = AacProfile.LC,
        // Antall kanaler (1 = mono, 2 = stereo)
        channels: Int? = null, // = 2,
        // Sample rate i Hz (typisk 44100 eller 48000)
        sampleRate: Int? = null
    ) : AudioCodec(codec = "aac", bitrate = bitrate, sampleRate = sampleRate, channels = channels) {
        override fun determineTranscodeDecision(stream: AudioStream): TranscodeDecision {
            val superDecision = super.determineTranscodeDecision(stream)
            if (superDecision == TranscodeDecision.Reencode) return superDecision
            if (forceCopy) return TranscodeDecision.Copy

            val profileOk = stream.profile.lowercase() == "lc"
            val channelsOk = stream.channels <= 6
            val sampleRateOk = stream.sample_rate.toIntOrNull() in listOf(44100, 48000)

            return when {
                !profileOk -> TranscodeDecision.Reencode // HE/HEv2 → reencode til LC
                !channelsOk || !sampleRateOk -> TranscodeDecision.Reencode
                else -> TranscodeDecision.Copy
            }
        }

        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val args = super.buildFfmpegArgs(suffix).toMutableList()
            if (profile != AacProfile.LC) {
                val s = suffix ?: ""
                args += listOf("-profile:a$s", profile.ffmpegName)
            }
            return args
        }

    }

    // MP3 (MPEG Layer III)
    class Mp3(
        bitrate: Int? = null, // = 192,
        channels: Int? = null, // = 2,
        sampleRate: Int? = null // = 44100
    ) : AudioCodec(codec = "libmp3lame", bitrate = bitrate, channels = channels, sampleRate = sampleRate) {}

    // Opus (moderne, lav latency, bra for streaming)
    class Opus(
        bitrate: Int? = null, // = 128,
        channels: Int? = null, // = 2,
        sampleRate: Int? = null, // = 48000,
        // Application mode: audio, voip, lowdelay
        var application: OpusApplication = OpusApplication.Audio
    ) : AudioCodec(codec = "opus", bitrate = bitrate, channels = channels, sampleRate = sampleRate) {
        override fun determineTranscodeDecision(stream: AudioStream): TranscodeDecision {
            val base = super.determineTranscodeDecision(stream)
            if (base == TranscodeDecision.Reencode) return base
            if (forceCopy) return TranscodeDecision.Copy

            // Opus må alltid være 48kHz internt, så hvis input != 48000 → reencode
            val sampleRateOk = stream.sample_rate.toIntOrNull() == 48000
            val channelsOk = (channels ?: stream.channels) <= 2 // typisk stereo

            return if (sampleRateOk && channelsOk) {
                TranscodeDecision.Copy
            } else {
                TranscodeDecision.Reencode
            }
        }

        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val args = super.buildFfmpegArgs(suffix).toMutableList()
            val s = suffix ?: ""
            args += listOf("-application", application.ffmpegName)
            return args
        }

    }

    // Vorbis (åpen kildekode, brukt i Ogg)
    class Vorbis(
        bitrate: Int? = null, // = 128,
        channels: Int? = null, // = 2,
        sampleRate: Int? = null, // = 44100
    ) : AudioCodec(codec = "libvorbis", bitrate = bitrate, channels = channels, sampleRate = sampleRate)

    // FLAC (lossless)
    class Flac(
        var compressionLevel: Int? = null, // = 5,
        channels: Int? = null, // = 2,
        sampleRate: Int? = null, // = 48000
    ) : AudioCodec(codec = "flac", channels = channels, sampleRate = sampleRate) {
        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val s = suffix ?: ""
            val args = mutableListOf("-c:a$s", "flac")
            compressionLevel?.let { args += listOf("-compression_level$s", it.toString()) }
            return args
        }

    }

    // AC3 (Dolby Digital)
    class Ac3(
        bitrate: Int? = null, // = 384,
        channels: Int? = null, // = 6,
        sampleRate: Int? = null, // = 48000
    ) : AudioCodec(codec = "ac3", bitrate = bitrate, channels = channels, sampleRate = sampleRate)

    class Eac3(
        bitrate: Int? = null,
        channels: Int? = null,
        sampleRate: Int? = null,
    ) : AudioCodec(codec = "eac3", bitrate =  bitrate, channels = channels, sampleRate = sampleRate)

    class Dts(
        bitrate: Int? = null,
        channels: Int? = null, // = 6,
        sampleRate: Int? = null, // = 48000
    ) : AudioCodec(codec = "dts", bitrate = bitrate, channels = channels, sampleRate = sampleRate)

    class Pcm(

    ): AudioCodec(codec = "pcm_s16le") {
        override fun buildFfmpegArgs(suffix: String?): List<String> {
            val s = suffix ?: ""
            return listOf("-c:a$s", "pcm_s16le")
        }

        override fun determineTranscodeDecision(stream: AudioStream) = TranscodeDecision.Reencode
    }


    // Kopier eksisterende audio uten reenkoding
    object Copy : AudioCodec("copy")

    var forceCopy: Boolean = false

    open fun determineTranscodeDecision(stream: AudioStream): TranscodeDecision {
        // 1) Hvis vi eksplisitt vil kopiere
        if (forceCopy || this == Copy) return TranscodeDecision.Copy

        // 2) Hvis codec er identisk og ingen parametre er satt → Copy
        val sameCodec = this.isSame(stream.codec_name)
        val wantsBitrateChange = bitrate != null
        val wantsSampleRateChange = sampleRate?.let { sr ->
            val inSr = stream.sample_rate.toIntOrNull()
            inSr != null && sr != inSr
        } ?: false
        val wantsChannelChange = channels?.let { ch ->
            ch != stream.channels
        } ?: false

        return when {
            // samme codec og ingen endringer → Copy
            sameCodec && !wantsBitrateChange && !wantsSampleRateChange && !wantsChannelChange ->
                TranscodeDecision.Copy

            // ellers → Reencode
            else -> TranscodeDecision.Reencode
        }
    }

    /**
     * Felles bygging av ffmpeg-argumenter.
     * - Tar hensyn til felter som er satt (bitrate, channels, sampleRate).
     * - Hopper over felter som er null.
     * - Validerer mot input-stream (ikke høyere enn input).
     */
    open fun buildFfmpegArgs(suffix: String? = null): List<String> {
        val s = suffix ?: ""
        val args = mutableListOf<String>()

        // codec
        args += listOf("-c:a$s", codec)

        // bitrate
        bitrate?.let { kbps ->
            args += listOf("-b:a$s", "${kbps}k")
        }

        // sample rate
        sampleRate?.let { sr ->
            args += listOf("-ar$s", sr.toString())
        }

        // channels
        channels?.let { ch ->
            args += listOf("-ac$s", ch.toString())
        }

        return args
    }


    fun setClamped(clamp: AudioClamp) {
        this.channels = clamp.channels ?: this.channels
        this.bitrate = clamp.bitrate ?: this.bitrate
        this.sampleRate = clamp.sampleRate ?: this.sampleRate
    }

    fun copy(): AudioCodec = when (this) {
        is Aac -> Aac(
            bitrate = this.bitrate,
            profile = this.profile,
            channels = this.channels,
            sampleRate = this.sampleRate
        )

        is Mp3 -> Mp3(
            bitrate = this.bitrate,
            channels = this.channels,
            sampleRate = this.sampleRate
        )

        is Opus -> Opus(
            bitrate = this.bitrate,
            channels = this.channels,
            sampleRate = this.sampleRate,
            application = this.application
        )

        is Vorbis -> Vorbis(
            bitrate = this.bitrate,
            channels = this.channels,
            sampleRate = this.sampleRate
        )

        is Flac -> Flac(
            compressionLevel = this.compressionLevel,
            channels = this.channels,
            sampleRate = this.sampleRate
        )

        is Ac3 -> Ac3(
            bitrate = this.bitrate,
            channels = this.channels,
            sampleRate = this.sampleRate
        )

        is Eac3 -> Eac3(
            bitrate = this.bitrate,
            channels = this.channels,
            sampleRate = this.sampleRate
        )

        is Dts -> Dts(
            bitrate = this.bitrate,
            channels = this.channels,
            sampleRate = this.sampleRate
        )

        is Pcm -> Pcm()

        Copy -> Copy
    }

}

fun AudioCodec.isSame(name: String): Boolean {
    val codecObject = when (name.lowercase()) {
        "aac", "mp4a", "libfdk_aac" -> AudioCodec.Aac()
        "mp3", "mpeg3", "libmp3lame" -> AudioCodec.Mp3()
        "opus", "libopus" -> AudioCodec.Opus()
        "vorbis", "oggvorbis", "libvorbis" -> AudioCodec.Vorbis()
        "flac" -> AudioCodec.Flac()
        "ac3", "dolby", "dolbydigital" -> AudioCodec.Ac3()
        "eac3", "ec3", "dolbydigitalplus", "ddp" -> AudioCodec.Eac3()
        "dts", "dca" -> AudioCodec.Dts()   // ← lagt til her
        "pcm_s16le", "pcm" -> AudioCodec.Pcm()
        "copy" -> AudioCodec.Copy
        else -> throw IllegalArgumentException("Unsupported audio codec: $name")
    }
    return (this.codec == codecObject.codec)
}


enum class AacProfile(val ffmpegName: String) {
    LC("aac_low"),   // Low Complexity – mest brukt
    HE("aac_he"),    // High Efficiency – bedre komprimering
    HEv2("aac_he_v2") // High Efficiency v2 – enda mer komprimering
}

enum class OpusApplication(val ffmpegName: String) {
    Audio("audio"),      // Vanlig musikk/lyd
    Voip("voip"),        // Optimalisert for tale
    LowDelay("lowdelay") // Lav latency, f.eks. live streaming
}
