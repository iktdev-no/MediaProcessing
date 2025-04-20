package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import no.iktdev.mediaprocessing.coordinator.log
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoArgumentsDto
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoPreference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoStream

class VideoArguments(
    val videoStream: VideoStream,
    val allStreams: ParsedMediaStreams,
    val preference: VideoPreference
) {
    fun isVideoCodecEqual() = getCodec(videoStream.codec_name) == getCodec(preference.codec.lowercase())
    fun getCodec(name: String): String {
        return when (name.lowercase()) {
            "hevc", "hevec", "h265", "h.265", "libx265" -> "libx265"
            "h.264", "h264", "libx264" -> "libx264"
            "vp9", "vp-9", "libvpx-vp9" -> "libvpx-vp9"
            "av1", "libaom-av1" -> "libaom-av1"
            "mpeg4", "mp4", "libxvid" -> "libxvid"
            "vvc", "h.266", "libvvc" -> "libvvc"
            "vp8", "libvpx" -> "libvpx"
            else -> name
        }
    }

    fun getCodec() = getCodec(videoStream.codec_name)


    fun getVideoArguments(): VideoArgumentsDto {
        val codecParams = if (isVideoCodecEqual()) {
            if (getCodec() == "libx265") {
                composeHevcArguments(getCodec())
            } else {
                mutableListOf("-c:v", "copy")
            }
        } else {
            when (getCodec(preference.codec.lowercase())) {
                "libx265" -> composeHevcArguments(getCodec())
                "libx264" -> composeH264Arguments(getCodec())
                else -> run {
                    val codec = getCodec(preference.codec.lowercase())
                    log.info { "Unsupported codec found ${codec}, making best effort..." }
                    listOf("-c:v", codec)
                }
            }
        }


        return VideoArgumentsDto(
            index = allStreams.videoStream.indexOf(videoStream),
            codecParameters = codecParams,
            optionalParameters = composeOptionalArguments()
        )
    }

    private fun composeOptionalArguments(): List<String> {

        val pixelFormat: List<String> = if (preference.pixelFormatPassthrough.none { it == videoStream.pix_fmt }) {
            listOf("-pix_fmt", preference.pixelFormat)
        } else emptyList()

        val crfParam = if (pixelFormat.isNotEmpty() || !isVideoCodecEqual()) {
            listOf("-crf", preference.threshold.toString())
        } else emptyList()

        val defaultCodecParams = listOf("-movflags", "+faststart")

        return pixelFormat + crfParam + defaultCodecParams
    }

    private fun composeH264Arguments(codec: String): List<String> {
        return listOf(
            "-c:v", "libx264",
            "-profile:v", "high",
            "-level:v", preference.h264Level.toString(),
            "preset", "slow",
        )
    }

    private fun composeHevcArguments(codec: String): List<String> {
        val targetProfile = if (videoStream.pix_fmt.contains("10")) "main10" else "main"

        val unsetCodecMetadata = videoStream.codec_tag_string == "[0][0][0][0]" || videoStream.codec_tag == "0x0000"

        // Map level til en streng – her forenklet
        val targetLevel = when (videoStream.level) {
            150 -> "5.0"
            153 -> "5.1"
            else -> "5.0" // Default hvis vi ikke har en eksplisitt mapping
        }

        return if (codec != "libx265" || (unsetCodecMetadata && preference.reencodeOnIncorrectMetadataForChromecast)) {
            // Konverter (eller reenkode) til HEVC med x265 med riktige parametere
            listOf(
                "-c:v", "libx265", "-preset", "slow",
                "-x265-params", "\"profile=$targetProfile:level=$targetLevel\"",
                "-tag:v", "hev1"
            )
        } else {
            // Dersom vi mener at vi kun trenger å remuxe, kan vi gjøre
            listOf(
                "-c:v", "copy", "-tag:v", "hev1"
            )
        }
    }


}