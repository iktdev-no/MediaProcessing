package no.iktdev.mediaprocessing.ffmpeg.dsl

import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.Disposition
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaPlanTest {

    @Test
    fun `video copy with one audio copy`() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Copy),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Copy))
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "aac", disposition = mockDisposition(), tags = mockTags()))
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "copy"
            ),
            args
        )
    }

    @Test
    fun `video reencode to hevc with crf`() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Hevc(crf = 18)),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Aac(bitrate = 192)))
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "mp3", disposition = mockDisposition(), tags = mockTags()))
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "libx265", "-crf", "18", "-preset", "slow",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "192k"
            ),
            args
        )
    }

    @Test
    fun `two audio tracks with different codecs`() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(0, AudioCodec.Aac(bitrate = 128)),
                AudioTarget(1, AudioCodec.Opus(bitrate = 96))
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(
                mockAudioStream(codec = "aac", channels = 6, disposition = mockDisposition(), tags = mockTags()),
                mockAudioStream(index = 1, codec = "ac3", disposition = mockDisposition(), tags = mockTags())
            )
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a:1", "-c:a:1", "opus", "-b:a:1", "96k", "-application", "audio"),
            args
        )
    }

    @Test
    fun `Video copy, Audio AAC reencode`() {

        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.H264()),
            audioTracks = mutableListOf(
                AudioTarget(0, AudioCodec.Aac(bitrate = 128, profile = AacProfile.LC)),
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(
                mockAudioStream(codec = "aac", channels = 6, profile = AacProfile.HE.ffmpegName, disposition = mockDisposition(), tags = mockTags()),
            )
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
            ),
            args
        )
    }

    @Test
    @DisplayName("Video copy + Audio AAC reencode (HE→LC, bitrate 128k)")
    fun videoCopyAudioAacReencode() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.H264()),
            audioTracks = mutableListOf(
                AudioTarget(0, AudioCodec.Aac(bitrate = 128, profile = AacProfile.LC)),
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "aac", channels = 6, profile = AacProfile.HE.ffmpegName, disposition = mockDisposition(), tags = mockTags()))
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
            ),
            args
        )
    }

    @Test
    @DisplayName("Video reencode to HEVC with CRF=18 and preset=slow, Audio copy")
    fun videoReencodeHevcCrfPresetAudioCopy() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Hevc(crf = 18, preset = Presets.Slow)),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Copy))
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "aac", channels = 2, profile = AacProfile.LC.ffmpegName, disposition = mockDisposition(), tags = mockTags()))
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "libx265", "-crf", "18", "-preset", "slow",
                "-map", "0:a:0", "-c:a:0", "copy",
            ),
            args
        )
    }

    @Test
    @DisplayName("Two audio tracks: AAC reencode 128k + Opus reencode 96k")
    fun twoAudioTracksDifferentCodecs() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(0, AudioCodec.Aac(bitrate = 128)),
                AudioTarget(1, AudioCodec.Opus(bitrate = 96))
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(
                mockAudioStream(codec = "aac", channels = 2, profile = AacProfile.LC.ffmpegName, disposition = mockDisposition(), tags = mockTags()),
                mockAudioStream(codec = "vorbis", channels = 2, profile = "", disposition = mockDisposition(), tags = mockTags())
            )
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a:1", "-c:a:1", "opus", "-b:a:1", "96k", "-application", "audio",
            ),
            args
        )
    }

    @Test
    @DisplayName("PCM input downmix to AAC stereo 192k")
    fun pcmInputDownmixToAacStereo() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Copy),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Aac(bitrate = 192, channels = 2)))
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "rawvideo", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "pcm_s16le", channels = 6, profile = "", disposition = mockDisposition(), tags = mockTags()))
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "192k", "-ac:0", "2",
            ),
            args
        )
    }

    @Test
    @DisplayName("FLAC input remux to FLAC (no reencode)")
    fun flacInputRemux() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Copy),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Flac()))
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "rawvideo", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "flac", channels = 2, profile = "", disposition = mockDisposition(), tags = mockTags()))
        )

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "copy",
            ),
            args
        )
    }


    @Test
    @DisplayName("Extended track skipped when same as default and default is copy")
    fun skipExtendedIfSameAsDefaultAndDefaultIsCopy() {
        // Arrange: lag en plan med default og extended som peker på samme input index
        val defaultTarget = AudioTarget(
            index = 0,
            codec = AudioCodec.Copy // default er copy
        )
        val extendedTarget = AudioTarget(
            index = 0, // peker på samme input index som default
            codec = AudioCodec.Copy
        )

        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Copy),
            audioTracks = mutableListOf(defaultTarget, extendedTarget)
        )

        val audioStreams = listOf(
            mockAudioStream(codec = "aac", channels = 2, disposition = mockDisposition(), tags = mockTags())
        )
        val videoStreams = listOf(
            mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())
        )

        // Act: bygg ffmpeg args
        val args = plan.toFfmpegArgs(videoStreams, audioStreams)

        // Assert: extended track skal være forkastet, kun ett audio map/codec skal finnes
        val expected = listOf(
            "-map", "0:v:0", "-c:v", "copy",
            "-map", "0:a:0", "-c:a:0", "copy",
        )
        assertEquals(expected, args)
    }

    @Test
    @DisplayName("""
    Hvis video=H264 og audio=AAC
    Når toContainer kalles
    Så:
        Returneres "mp4"
    """)
    fun testChooseContainerMp4() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.H264()),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Aac(channels = 2)))
        )
        assertEquals("mp4", plan.toContainer())
    }

    @Test
    @DisplayName("""
    Hvis video=VP9 og audio=Opus
    Når toContainer kalles
    Så:
        Returneres "webm"
    """)
    fun testChooseContainerWebm() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Vp9()),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Opus()))
        )
        assertEquals("webm", plan.toContainer())
    }


    @Test
    @DisplayName("""
    Hvis video=AV1 og audio=FLAC
    Når toContainer kalles
    Så:
        Returneres "mkv" (fallback)
    """)
    fun testChooseContainerMkv() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Av1()),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Flac()))
        )
        assertEquals("mkv", plan.toContainer())
    }


    @Test
    @DisplayName("""
        Hvis video=HEVC og audio=AAC
        Når chooseContainer kalles
        Så:
            Returneres "mp4"
    """)
    fun testHevcWithAacGivesMp4() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Aac(channels = 2)))
        )
        assertEquals("mp4", plan.toContainer())
    }

    @Test
    @DisplayName("""
        Hvis video=HEVC og audio=AC3
        Når chooseContainer kalles
        Så:
            Returneres "mkv" (fallback, siden AC3 ikke støttes i MP4)
    """)
    fun testHevcWithAc3GivesMkv() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Ac3()))
        )
        assertEquals("mkv", plan.toContainer())
    }

    @Test
    @DisplayName("""
        Hvis video=HEVC og audio=DTS
        Når chooseContainer kalles
        Så:
            Returneres "mkv" (fallback, siden DTS ikke støttes i MP4)
    """)
    fun testHevcWithDtsGivesMkv() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, AudioCodec.Dts()))
        )
        assertEquals("mkv", plan.toContainer())
    }


    fun mockVideoStream(
        index: Int = 0,
        codec: String = "h264",
        width: Int = 1920,
        height: Int = 1080,
        disposition: Disposition,
        tags: Tags
    ) = VideoStream(
        index = index,
        codec_name = codec,
        codec_long_name = "H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10",
        codec_type = "video",
        codec_tag_string = "avc1",
        codec_tag = "0x31637661",
        r_frame_rate = "25/1",
        avg_frame_rate = "25/1",
        time_base = "1/90000",
        start_pts = 0,
        start_time = "0.000000",
        disposition = disposition,
        tags = tags,
        duration = "60.0",
        duration_ts = 54000,
        profile = "High",
        width = width,
        height = height,
        coded_width = width,
        coded_height = height,
        closed_captions = 0,
        has_b_frames = 2,
        sample_aspect_ratio = "1:1",
        display_aspect_ratio = "16:9",
        pix_fmt = "yuv420p",
        level = 40,
        color_range = "tv",
        color_space = "bt709",
        color_transfer = "bt709",
        color_primaries = "bt709",
        chroma_location = "left",
        refs = 1
    )

    fun mockAudioStream(
        index: Int = 0,
        codec: String = "aac",
        channels: Int = 2,
        profile: String = "LC",
        disposition: Disposition,
        tags: Tags
    ) = AudioStream(
        index = index,
        codec_name = codec,
        codec_long_name = "AAC (Advanced Audio Coding)",
        codec_type = "audio",
        codec_tag_string = "mp4a",
        codec_tag = "0x6134706d",
        r_frame_rate = "0/0",
        avg_frame_rate = "0/0",
        time_base = "1/48000",
        start_pts = 0,
        start_time = "0.000000",
        duration = "60.0",
        duration_ts = 2880000,
        disposition = disposition,
        tags = tags,
        profile = profile,
        sample_fmt = "fltp",
        sample_rate = "48000",
        channels = channels,
        channel_layout = "stereo",
        bits_per_sample = 0
    )

    fun mockDisposition(
        default: Int = 1,
        forced: Int = 0
    ) = Disposition(
        default = default,
        dub = 0,
        original = 0,
        comment = 0,
        lyrics = 0,
        karaoke = 0,
        forced = forced,
        hearing_impaired = 0,
        captions = 0,
        visual_impaired = 0,
        clean_effects = 0,
        attached_pic = 0,
        timed_thumbnails = 0
    )

    fun mockTags(
        language: String? = "eng",
        title: String? = null,
        filename: String? = null
    ) = Tags(
        title = title,
        BPS = null,
        DURATION = null,
        NUMBER_OF_FRAMES = 0,
        NUMBER_OF_BYTES = null,
        _STATISTICS_WRITING_APP = null,
        _STATISTICS_WRITING_DATE_UTC = null,
        _STATISTICS_TAGS = null,
        language = language,
        filename = filename,
        mimetype = null
    )


}