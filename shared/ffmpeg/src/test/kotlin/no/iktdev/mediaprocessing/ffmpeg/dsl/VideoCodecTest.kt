package no.iktdev.mediaprocessing.ffmpeg.dsl

import com.google.gson.Gson
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VideoCodecTest {

    @Test
    fun verifyCopy1() {
        val videoStream = Gson().fromJson(hevcExpectsVideoCopy, VideoStream::class.java)
        val decision = VideoCodec.Hevc().determineTranscodeDecision(videoStream)
        assertEquals(TranscodeDecision.Copy, decision)
    }



    val hevcExpectsVideoCopy = """
        {
          "index": 0,
          "codec_name": "hevc",
          "codec_long_name": "H.265 / HEVC (High Efficiency Video Coding)",
          "profile": "Main 10",
          "codec_type": "video",
          "codec_time_base": "1001/24000",
          "codec_tag_string": "[0][0][0][0]",
          "codec_tag": "0x0000",
          "width": 3840,
          "height": 2160,
          "coded_width": 3840,
          "coded_height": 2160,
          "closed_captions": 0,
          "has_b_frames": 2,
          "sample_aspect_ratio": "1:1",
          "display_aspect_ratio": "16:9",
          "pix_fmt": "yuv420p10le",
          "level": 150,
          "color_range": "tv",
          "color_space": "bt709",
          "color_transfer": "bt709",
          "color_primaries": "bt709",
          "refs": 1,
          "r_frame_rate": "24000/1001",
          "avg_frame_rate": "24000/1001",
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
            "language": "jpn",
            "BPS": "24613148",
            "DURATION": "01:39:59.494000000",
            "NUMBER_OF_FRAMES": "143844",
            "NUMBER_OF_BYTES": "18458304767",
            "_STATISTICS_WRITING_APP": "mkvmerge v96.0 ('It's My Life') 64-bit",
            "_STATISTICS_TAGS": "BPS DURATION NUMBER_OF_FRAMES NUMBER_OF_BYTES"
          }
        }
    """.trimIndent()
}