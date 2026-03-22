package no.iktdev.mediaprocessing.ffmpeg.util

import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.model.AudioClamp
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.model.SelectedAudioTracks

class AudioTargeting(private val audioStreams: List<AudioStream>) {

    fun getAudioTargets(
        audioTracks: List<SelectedAudioTracks>,
        preference: AudioCodec,
        extendedPreference: AudioCodec? = null,
    ): List<AudioTarget> {

        val targets = mutableListOf<AudioTarget>()

        for (track in audioTracks) {

            // --- DEFAULT ---
            val defaultStream = audioStreams.firstOrNull { it.index == track.defaultFfmpegIndex }
                ?: error("No audio stream with index=${track.defaultListIndex}. Available: ${audioStreams.map { it.index }}")

            val defaultCodec = preference.copy().apply {
                val clamp = getAudioClamp(this, defaultStream)
                setClamped(clamp)
            }

            targets += AudioTarget(
                listIndex = track.defaultListIndex,
                ffmpegIndex = track.defaultFfmpegIndex,
                codec = defaultCodec
            )


            // --- EXTENDED ---
            if (track.extendedListIndex != null && track.extendedFfmpegIndex != null) {

                val extStream = audioStreams.firstOrNull { it.index == track.extendedFfmpegIndex }
                    ?: error("No audio stream with ffmpegIndex=${track.defaultFfmpegIndex}. Available: ${audioStreams.map { it.index }}")


                val extCodec = (extendedPreference ?: preference).copy().apply {
                    val clamp = getAudioClamp(this, extStream)
                    setClamped(clamp)
                }

                targets += AudioTarget(
                    listIndex = track.extendedListIndex,
                    ffmpegIndex = track.extendedFfmpegIndex,
                    codec = extCodec
                )
            }
        }

        return targets
    }


    fun getAudioClamp(codec: AudioCodec, stream: AudioStream): AudioClamp {
        val finalBitrate = codec.bitrate?.let { req ->
            val src = (stream.bit_rate ?: (req * 1000)).toInt() / 1000
            minOf(req, src)
        }

        val finalSampleRate = codec.sampleRate?.let { req ->
            val src = stream.sample_rate.toIntOrNull() ?: req
            minOf(req, src)
        }

        val finalChannels = codec.channels?.let { req ->
            minOf(req, stream.channels)
        }

        return AudioClamp(
            bitrate = finalBitrate,
            sampleRate = finalSampleRate,
            channels = finalChannels
        )
    }
}
