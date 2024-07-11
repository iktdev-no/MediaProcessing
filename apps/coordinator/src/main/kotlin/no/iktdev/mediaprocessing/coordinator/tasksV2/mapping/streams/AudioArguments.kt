package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioArgumentsDto
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioPreference
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams

class AudioArguments(
    val audioStream: AudioStream,
    val allStreams: ParsedMediaStreams,
    val preference: AudioPreference
) {
    fun isAudioCodecEqual() = audioStream.codec_name.lowercase() == preference.codec.lowercase()

    fun isSurroundButNotEAC3(): Boolean {
        return audioStream.channels > 2 && audioStream.codec_name.lowercase() != "eac3"
    }

    fun isSurroundAndEAC3(): Boolean {
        return audioStream.channels > 2 && audioStream.codec_name.lowercase() == "eac3"
    }

    fun isSurround(): Boolean {
        return audioStream.channels > 2
    }

    private fun shouldUseEAC3(): Boolean {
        return (preference.defaultToEAC3OnSurroundDetected && audioStream.channels > 2 && audioStream.codec_name.lowercase() != "eac3")
    }

    fun getAudioArguments(): AudioArgumentsDto {
        val optionalParams = mutableListOf<String>()

        val codecParams = if (isAudioCodecEqual() || isSurroundAndEAC3()) {
            listOf("-acodec", "copy")
        } else if (!isSurroundButNotEAC3() && shouldUseEAC3()) {
            listOf("-c:a", "eac3")
        } else {
            val codecSwap = mutableListOf("-c:a", preference.codec)
            if (audioStream.channels > 2 && !preference.preserveChannels) {
                codecSwap.addAll(listOf("-ac", "2"))
            }
            codecSwap
        }

        return AudioArgumentsDto(
            index = allStreams.audioStream.indexOf(audioStream),
            codecParameters = codecParams,
            optionalParameters = optionalParams
        )
    }

}