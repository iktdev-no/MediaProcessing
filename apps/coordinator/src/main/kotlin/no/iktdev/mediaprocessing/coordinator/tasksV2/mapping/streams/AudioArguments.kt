package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.AudioArgumentsDto
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.AudioPreference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams

class AudioArguments(
    val audioStream: AudioStream,
    val allStreams: ParsedMediaStreams,
    val preference: AudioPreference
) {
    fun isAudioCodecEqual() = audioStream.codec_name.lowercase() == preference.codec.lowercase()

    /**
     * Checks whether its ac3 or eac3, as google cast supports this and most other devices.
     */
    fun isOfSupportedSurroundCodec(): Boolean {
        return audioStream.codec_name.lowercase() in listOf("eac3", "ac3")
    }


    fun isSurround(): Boolean {
        return audioStream.channels > 2
    }


    fun getAudioArguments(): AudioArgumentsDto {
        val optionalParams = mutableListOf<String>()

        if (!isSurround()) {
            return if (isAudioCodecEqual()) {
                asPassthrough()
            } else {
                asStereo()
            }
        } else {
            if (preference.forceStereo) {
                return asStereo()
            }
            if (preference.passthroughOnGenerallySupportedSurroundSound && isOfSupportedSurroundCodec()) {
                return asPassthrough()
            } else if (preference.passthroughOnGenerallySupportedSurroundSound && !isOfSupportedSurroundCodec()) {
                return asSurround()
            }
            if (preference.convertToEac3OnUnsupportedSurround ) {
                return asSurround()
            }


            return asStereo()
        }
    }

    fun index(): Int {
        return allStreams.audioStream.indexOf(audioStream)
    }

    fun asPassthrough(): AudioArgumentsDto {
        return AudioArgumentsDto(
            index = index()
        )
    }
    fun asStereo(): AudioArgumentsDto {
        return AudioArgumentsDto(
            index = index(),
            codecParameters = listOf(
                "-c:a", preference.codec,
                "-ac", "2"
            ), optionalParameters = emptyList()
        )
    }
    fun asSurround(): AudioArgumentsDto {
        return AudioArgumentsDto(
            index = index(),
            codecParameters = listOf(
                "-c:a", "eac3"
            ), optionalParameters = emptyList()
        )
    }
}