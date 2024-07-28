package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.common.Preference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.AudioPreference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.AudioStream
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AudioArgumentsTest {

    @Test
    fun validateChecks1() {
        val data = Gson().fromJson(ac3AudioStreamJson, AudioStream::class.java)
        val audioArguments = AudioArguments(
            allStreams = ParsedMediaStreams(
                audioStream = listOf(data), videoStream = emptyList(), subtitleStream = emptyList()),
            preference = Preference.getPreference().encodePreference.audio,
            audioStream = data
        )
        assertThat(audioArguments.isAudioCodecEqual()).isFalse()
        assertThat(audioArguments.isSurround()).isTrue()

        assertThat(audioArguments.getAudioArguments().codecParameters).isEqualTo(listOf("-acodec", "copy"))
    }

    @Test
    fun validateChecks2() {
        val data = Gson().fromJson(eac3AudioStreamJson, AudioStream::class.java)
        val audioArguments = AudioArguments(
            allStreams = ParsedMediaStreams(
                audioStream = listOf(data), videoStream = emptyList(), subtitleStream = emptyList()),
            preference = Preference.getPreference().encodePreference.audio,
            audioStream = data
        )
        assertThat(audioArguments.isAudioCodecEqual()).isFalse()
        assertThat(audioArguments.isSurround()).isTrue()

        assertThat(audioArguments.getAudioArguments().codecParameters).isEqualTo(listOf("-acodec", "copy"))
    }

    @Test
    fun validateChecks3() {
        val data = Gson().fromJson(aacSurroundAudioStreamJson, AudioStream::class.java)
        val audioArguments = AudioArguments(
            allStreams = ParsedMediaStreams(
                audioStream = listOf(data), videoStream = emptyList(), subtitleStream = emptyList()),
            preference = Preference.getPreference().encodePreference.audio,
            audioStream = data
        )
        assertThat(audioArguments.isAudioCodecEqual()).isTrue()
        assertThat(audioArguments.isSurround()).isTrue()

        assertThat(audioArguments.getAudioArguments().codecParameters).isEqualTo(listOf("-c:a", "eac3"))
    }

    @Test
    fun validateChecks4() {
        val data = Gson().fromJson(aacStereoAudioStreamJson, AudioStream::class.java)
        val audioArguments = AudioArguments(
            allStreams = ParsedMediaStreams(
                audioStream = listOf(data), videoStream = emptyList(), subtitleStream = emptyList()),
            preference = Preference.getPreference().encodePreference.audio,
            audioStream = data
        )
        assertThat(audioArguments.isAudioCodecEqual()).isTrue()
        assertThat(audioArguments.isSurround()).isFalse()

        assertThat(audioArguments.getAudioArguments().codecParameters).isEqualTo(listOf("-acodec", "copy"))
    }


    val ac3AudioStreamJson = """
        {
            "index": 1,
            "codec_name": "ac3",
            "codec_long_name": "ATSC A/52A (AC-3)",
            "codec_type": "audio",
            "codec_tag_string": "[0][0][0][0]",
            "codec_tag": "0x0000",
            "sample_fmt": "fltp",
            "sample_rate": "48000",
            "channels": 6,
            "channel_layout": "5.1(side)",
            "bits_per_sample": 0,
            "dmix_mode": "-1",
            "ltrt_cmixlev": "-1.000000",
            "ltrt_surmixlev": "-1.000000",
            "loro_cmixlev": "-1.000000",
            "loro_surmixlev": "-1.000000",
            "r_frame_rate": "0/0",
            "avg_frame_rate": "0/0",
            "time_base": "1/1000",
            "start_pts": -5,
            "start_time": "-0.005000",
            "bit_rate": "448000",
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
                "language": "eng",
                "ENCODER": "Lavc60.31.102 ac3",
                "DURATION": "01:05:13.024000000"
            }
        }
    """.trimIndent()
    val eac3AudioStreamJson = """
        {
            "index": 1,
            "codec_name": "eac3",
            "codec_long_name": "E-AC3",
            "codec_type": "audio",
            "codec_tag_string": "[0][0][0][0]",
            "codec_tag": "0x0000",
            "sample_fmt": "fltp",
            "sample_rate": "48000",
            "channels": 6,
            "channel_layout": "5.1(side)",
            "bits_per_sample": 0,
            "dmix_mode": "-1",
            "ltrt_cmixlev": "-1.000000",
            "ltrt_surmixlev": "-1.000000",
            "loro_cmixlev": "-1.000000",
            "loro_surmixlev": "-1.000000",
            "r_frame_rate": "0/0",
            "avg_frame_rate": "0/0",
            "time_base": "1/1000",
            "start_pts": -5,
            "start_time": "-0.005000",
            "bit_rate": "448000",
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
                "language": "eng",
                "ENCODER": "Lavc60.31.102 ac3",
                "DURATION": "01:05:13.024000000"
            }
        }
    """.trimIndent()
    val aacStereoAudioStreamJson = """
        {
            "index": 1,
            "codec_name": "aac",
            "codec_long_name": "AAC (Advanced Audio Coding)",
            "profile": "LC",
            "codec_type": "audio",
            "codec_tag_string": "mp4a",
            "codec_tag": "0x6134706d",
            "sample_fmt": "fltp",
            "sample_rate": "48000",
            "channels": 2,
            "channel_layout": "2",
            "bits_per_sample": 0,
            "r_frame_rate": "0/0",
            "avg_frame_rate": "0/0",
            "time_base": "1/48000",
            "start_pts": 0,
            "start_time": "0.000000",
            "duration_ts": 376210896,
            "duration": "7837.727000",
            "bit_rate": "224000",
            "nb_frames": "367396",
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
                "creation_time": "2022-02-08T20:37:35.000000Z",
                "language": "eng",
                "handler_name": "SoundHandler",
                "vendor_id": "[0][0][0][0]"
            }
        }
    """.trimIndent()

    val aacSurroundAudioStreamJson = """
        {
            "index": 1,
            "codec_name": "aac",
            "codec_long_name": "AAC (Advanced Audio Coding)",
            "profile": "LC",
            "codec_type": "audio",
            "codec_tag_string": "mp4a",
            "codec_tag": "0x6134706d",
            "sample_fmt": "fltp",
            "sample_rate": "48000",
            "channels": 6,
            "channel_layout": "5.1",
            "bits_per_sample": 0,
            "r_frame_rate": "0/0",
            "avg_frame_rate": "0/0",
            "time_base": "1/48000",
            "start_pts": 0,
            "start_time": "0.000000",
            "duration_ts": 376210896,
            "duration": "7837.727000",
            "bit_rate": "224000",
            "nb_frames": "367396",
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
                "creation_time": "2022-02-08T20:37:35.000000Z",
                "language": "eng",
                "handler_name": "SoundHandler",
                "vendor_id": "[0][0][0][0]"
            }
        }
    """.trimIndent()
}