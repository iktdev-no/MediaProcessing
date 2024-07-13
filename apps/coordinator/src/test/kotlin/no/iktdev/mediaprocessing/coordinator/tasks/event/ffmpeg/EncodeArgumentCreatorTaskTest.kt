package no.iktdev.mediaprocessing.coordinator.tasks.event.ffmpeg

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams.AudioArguments
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioPreference
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EncodeArgumentCreatorTaskTest {

    @Test
    fun verifyThatEacStreamGetsCorrectArguments() {
        val audio = AudioArguments(
            audioStream = audioStreamsEAC().first(),
            allStreams = ParsedMediaStreams(listOf(), audioStreamsEAC(), listOf()),
            preference = AudioPreference(preserveChannels = true, forceStereo = false, defaultToEAC3OnSurroundDetected = true)
        )
        val arguments = audio.getAudioArguments()
        assertThat(arguments.codecParameters).isEqualTo(listOf("-acodec", "copy"))

    }

    private fun audioStreamsEAC(): List<AudioStream> {
        //language=json
        val streams = """
            
          [
            {
                "index": 1,
                "codec_name": "eac3",
                "codec_long_name": "ATSC A/52B (AC-3, E-AC-3)",
                "codec_type": "audio",
                "codec_tag_string": "[0][0][0][0]",
                "codec_tag": "0x0000",
                "r_frame_rate": "0/0",
                "avg_frame_rate": "0/0",
                "time_base": "1/1000",
                "start_pts": 0,
                "start_time": "0.000000",
                "disposition": {
                    "default": 1,
                    "dub": 0,
                    "original": 0,
                    "comment": 0,
                    "lyrics": 0,
                    "karaoke": 0,
                    "forced": 0,
                    "hearing_impaired": 0,
                    "visual_impaired": 0,
                    "clean_effects": 0,
                    "attached_pic": 0,
                    "timed_thumbnails": 0
                },
                "tags": {
                    "BPS": "256000",
                    "DURATION": "01:09:55.296000000",
                    "NUMBER_OF_FRAMES": "131103",
                    "NUMBER_OF_BYTES": "134249472",
                    "_STATISTICS_WRITING_APP": "64-bit",
                    "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES",
                    "language": "eng"
                },
                "sample_fmt": "fltp",
                "sample_rate": "48000",
                "channels": 6,
                "bits_per_sample": 0
            }
        ]
            
        """.trimIndent()
        val type = object : TypeToken<List<AudioStream>>() {}.type
        return Gson().fromJson<List<AudioStream>>(streams, type)
    }
}