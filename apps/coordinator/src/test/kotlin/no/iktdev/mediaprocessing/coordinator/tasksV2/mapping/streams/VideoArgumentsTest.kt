package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams

import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.common.Preference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoPreference
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.VideoStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class VideoArgumentsTest {

    private fun getVideoArguments(videoStream: VideoStream, preference: VideoPreference): VideoArguments {
        return VideoArguments(
            allStreams = ParsedMediaStreams(
                videoStream = listOf(videoStream),
                audioStream = emptyList(),
                subtitleStream = emptyList()
            ),
            preference = preference,
            videoStream = videoStream
        )
    }

    @Test
    fun hevcStream1() {
        val data = Gson().fromJson(hevcStream1, VideoStream::class.java)
        val videoArguments = getVideoArguments(data,
            Preference.getPreference().encodePreference.video
                .copy(codec = "h265")
        )
        assertThat(videoArguments.isVideoCodecEqual()).isTrue()
        assertThat(videoArguments.getCodec()).isEqualTo("libx265")
        assertThat(videoArguments.getVideoArguments().codecParameters.take(2)).isEqualTo(listOf("-c:v", "copy"))
    }

    @Test
    @DisplayName("""
        When a hevc encoded media file gets received,
        But it has unset metadata, and re-encode for chromecast is set,
        Then the parameters should not specify copy
    """)
    fun hevcStream2() {
        val data = Gson().fromJson(hevcStream2, VideoStream::class.java)
        val videoArguments = getVideoArguments(data,
            Preference.getPreference().encodePreference.video
                .copy(codec = "h265", reencodeOnIncorrectMetadataForChromecast = true)
        )
        assertThat(videoArguments.isVideoCodecEqual()).isTrue()
        assertThat(videoArguments.getCodec()).isEqualTo("libx265")
        assertThat(videoArguments.getVideoArguments().codecParameters.take(2)).isEqualTo(listOf("-c:v", "libx265"))
    }

    @Test
    @DisplayName("""
        When a vc1 encoded media file gets received
        And preference is set to hevc,
        Then the parameters should be to encode in hevc
    """)
    fun vc1Stream1() {
        val data = Gson().fromJson(vc1Stream, VideoStream::class.java)
        val videoArguments = getVideoArguments(data,
            Preference.getPreference().encodePreference.video
                .copy(codec = "h265", reencodeOnIncorrectMetadataForChromecast = true)
        )
        assertThat(videoArguments.isVideoCodecEqual()).isFalse()
        assertThat(videoArguments.getCodec()).isEqualTo("vc1")
        assertThat(videoArguments.getVideoArguments().codecParameters.take(2)).isEqualTo(listOf("-c:v", "libx265"))
    }

    @Test
    @DisplayName("""
        When a vc1 encoded media file gets received
        And preference is set to h264,
        Then the parameters should be to encode in h264
    """)
    fun vc1Stream2() {
        val data = Gson().fromJson(vc1Stream, VideoStream::class.java)
        val videoArguments = getVideoArguments(data,
            Preference.getPreference().encodePreference.video
                .copy(codec = "h264", reencodeOnIncorrectMetadataForChromecast = true)
        )
        assertThat(videoArguments.isVideoCodecEqual()).isFalse()
        assertThat(videoArguments.getCodec()).isEqualTo("vc1")
        assertThat(videoArguments.getVideoArguments().codecParameters.take(2)).isEqualTo(listOf("-c:v", "libx264"))
    }

    @Test
    fun h264Stream1() {
        val data = Gson().fromJson(h264stream1, VideoStream::class.java)
        val videoArguments = getVideoArguments(data,
            Preference.getPreference().encodePreference.video
                .copy(codec = "h265", reencodeOnIncorrectMetadataForChromecast = true)
        )
        assertThat(videoArguments.isVideoCodecEqual()).isFalse()
        assertThat(videoArguments.getCodec()).isEqualTo("libx264")
        assertThat(videoArguments.getVideoArguments().codecParameters.take(2)).isEqualTo(listOf("-c:v", "libx265"))
    }

    @Test
    fun h264Stream2() {
        val data = Gson().fromJson(h264stream1, VideoStream::class.java)
        val videoArguments = getVideoArguments(data,
            Preference.getPreference().encodePreference.video
                .copy(reencodeOnIncorrectMetadataForChromecast = true)
        )
        assertThat(videoArguments.isVideoCodecEqual()).isTrue()
        assertThat(videoArguments.getCodec()).isEqualTo("libx264")
        assertThat(videoArguments.getVideoArguments().codecParameters.take(2)).isEqualTo(listOf("-c:v", "copy"))
    }

    val hevcStream1 = """
        {
            "index": 0,
            "codec_name": "hevc",
            "codec_long_name": "H.265 / HEVC (High Efficiency Video Coding)",
            "profile": "Main 10",
            "codec_type": "video",
            "codec_tag_string": "hev1",
            "codec_tag": "0x31766568",
            "width": 1920,
            "height": 1080,
            "coded_width": 1920,
            "coded_height": 1080,
            "closed_captions": 0,
            "film_grain": 0,
            "has_b_frames": 2,
            "sample_aspect_ratio": "1:1",
            "display_aspect_ratio": "16:9",
            "pix_fmt": "yuv420p10le",
            "level": 150,
            "color_range": "tv",
            "chroma_location": "left",
            "refs": 1,
            "id": "0x1",
            "r_frame_rate": "24000/1001",
            "avg_frame_rate": "34045000/1419959",
            "time_base": "1/16000",
            "start_pts": 0,
            "start_time": "0.000000",
            "duration_ts": 22719344,
            "duration": "1419.959000",
            "bit_rate": "2020313",
            "nb_frames": "34045",
            "extradata_size": 2535,
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
                "timed_thumbnails": 0,
                "non_diegetic": 0,
                "captions": 0,
                "descriptions": 0,
                "metadata": 0,
                "dependent": 0,
                "still_image": 0
            },
            "tags": {
                "language": "jpn",
                "handler_name": "VideoHandler",
                "vendor_id": "[0][0][0][0]"
            }
        }
    """.trimIndent()

    val hevcStream2 = """
        {
            "index": 0,
            "codec_name": "hevc",
            "codec_long_name": "H.265 / HEVC (High Efficiency Video Coding)",
            "profile": "Main 10",
            "codec_type": "video",
            "codec_tag_string": "[0][0][0][0]",
            "codec_tag": "0x0000",
            "width": 1920,
            "height": 1080,
            "coded_width": 1920,
            "coded_height": 1080,
            "closed_captions": 0,
            "film_grain": 0,
            "has_b_frames": 2,
            "sample_aspect_ratio": "1:1",
            "display_aspect_ratio": "16:9",
            "pix_fmt": "yuv420p10le",
            "level": 150,
            "color_range": "tv",
            "chroma_location": "left",
            "refs": 1,
            "r_frame_rate": "24000/1001",
            "avg_frame_rate": "24000/1001",
            "time_base": "1/1000",
            "start_pts": 0,
            "start_time": "0.000000",
            "extradata_size": 2535,
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
                "timed_thumbnails": 0,
                "non_diegetic": 0,
                "captions": 0,
                "descriptions": 0,
                "metadata": 0,
                "dependent": 0,
                "still_image": 0
            },
            "tags": {
                "language": "jpn",
                "title": "nan",
                "BPS": "2105884",
                "DURATION": "00:53:27.204000000",
                "NUMBER_OF_FRAMES": "76896",
                "NUMBER_OF_BYTES": "844250247",
                "_STATISTICS_WRITING_APP": "mkvmerge v91.0 ('Signs') 64-bit",
                "_STATISTICS_WRITING_DATE_UTC": "2025-03-31 17:33:38",
                "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
            }
        }
    """.trimIndent()


    val vc1Stream = """
        {
            "index": 0,
            "codec_name": "vc1",
            "codec_long_name": "SMPTE VC-1",
            "profile": "Advanced",
            "codec_type": "video",
            "codec_tag_string": "WVC1",
            "codec_tag": "0x31435657",
            "width": 1920,
            "height": 1080,
            "coded_width": 1920,
            "coded_height": 1080,
            "closed_captions": 0,
            "film_grain": 0,
            "has_b_frames": 1,
            "sample_aspect_ratio": "1:1",
            "display_aspect_ratio": "16:9",
            "pix_fmt": "yuv420p",
            "level": 3,
            "chroma_location": "left",
            "field_order": "progressive",
            "refs": 1,
            "r_frame_rate": "24000/1001",
            "avg_frame_rate": "24000/1001",
            "time_base": "1/1000",
            "start_pts": 0,
            "start_time": "0.000000",
            "duration_ts": 5189856,
            "duration": "5189.856000",
            "extradata_size": 34,
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
                "timed_thumbnails": 0,
                "non_diegetic": 0,
                "captions": 0,
                "descriptions": 0,
                "metadata": 0,
                "dependent": 0,
                "still_image": 0
            },
            "tags": {
                "title": ""
            }
        }
    """.trimIndent()

    val h264stream1 = """
        {
            "index": 0,
            "codec_name": "h264",
            "codec_long_name": "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
            "profile": "High",
            "codec_type": "video",
            "codec_tag_string": "avc1",
            "codec_tag": "0x31637661",
            "width": 1920,
            "height": 1080,
            "coded_width": 1920,
            "coded_height": 1080,
            "closed_captions": 0,
            "film_grain": 0,
            "has_b_frames": 2,
            "sample_aspect_ratio": "1:1",
            "display_aspect_ratio": "16:9",
            "pix_fmt": "yuv420p",
            "level": 40,
            "chroma_location": "left",
            "field_order": "progressive",
            "refs": 1,
            "is_avc": "true",
            "nal_length_size": "4",
            "id": "0x1",
            "r_frame_rate": "30000/1001",
            "avg_frame_rate": "30000/1001",
            "time_base": "1/30000",
            "start_pts": 0,
            "start_time": "0.000000",
            "duration_ts": 185519300,
            "duration": "6183.976667",
            "bit_rate": "6460534",
            "bits_per_raw_sample": "8",
            "nb_frames": "185334",
            "extradata_size": 45,
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
                "timed_thumbnails": 0,
                "non_diegetic": 0,
                "captions": 0,
                "descriptions": 0,
                "metadata": 0,
                "dependent": 0,
                "still_image": 0
            },
            "tags": {
                "creation_time": "2018-07-09T06:20:13.000000Z",
                "language": "und",
                "handler_name": "nah",
                "vendor_id": "[0][0][0][0]"
            }
        }
    """.trimIndent()
}