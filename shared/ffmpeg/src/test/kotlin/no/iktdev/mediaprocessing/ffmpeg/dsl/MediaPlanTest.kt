package no.iktdev.mediaprocessing.ffmpeg.dsl

import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.Disposition
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaPlanTest {

    @Test
    fun `video copy with one audio copy`() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Copy)
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "aac", disposition = mockDisposition(), tags = mockTags()))
        )

        assertEquals(
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
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Hevc(crf = 18)),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Aac(bitrate = 192))
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "mp3", disposition = mockDisposition(), tags = mockTags()))
        )

        assertEquals(
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
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Aac(bitrate = 128)),
                AudioTarget(listIndex = 1, ffmpegIndex = 1, codec = AudioCodec.Opus(bitrate = 96))
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(
                mockAudioStream(codec = "aac", channels = 6, disposition = mockDisposition(), tags = mockTags()),
                mockAudioStream(index = 1, codec = "ac3", disposition = mockDisposition(), tags = mockTags())
            )
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a:1", "-c:a:1", "opus", "-b:a:1", "96k", "-application", "audio"
            ),
            args
        )
    }

    @Test
    fun `Video copy, Audio AAC reencode`() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.H264()),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Aac(bitrate = 128, profile = AacProfile.LC)),
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(
                mockAudioStream(codec = "aac", channels = 6, profile = AacProfile.HE.ffmpegName, disposition = mockDisposition(), tags = mockTags()),
            )
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k"
            ),
            args
        )
    }

    @Test
    @DisplayName("Video copy + Audio AAC reencode (HE→LC, bitrate 128k)")
    fun videoCopyAudioAacReencode() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.H264()),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Aac(bitrate = 128, profile = AacProfile.LC)),
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "aac", channels = 6, profile = AacProfile.HE.ffmpegName, disposition = mockDisposition(), tags = mockTags()))
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k"
            ),
            args
        )
    }

    @Test
    @DisplayName("Video reencode to HEVC with CRF=18 and preset=slow, Audio copy")
    fun videoReencodeHevcCrfPresetAudioCopy() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Hevc(crf = 18, preset = Presets.Slow)),
            audioTracks = mutableListOf(AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Copy))
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "aac", channels = 2, profile = AacProfile.LC.ffmpegName, disposition = mockDisposition(), tags = mockTags()))
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "libx265", "-crf", "18", "-preset", "slow",
                "-map", "0:a:0", "-c:a:0", "copy"
            ),
            args
        )
    }

    @Test
    @DisplayName("Two audio tracks: AAC reencode 128k + Opus reencode 96k")
    fun twoAudioTracksDifferentCodecs() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Aac(bitrate = 128)),
                AudioTarget(listIndex = 1, ffmpegIndex = 1, codec = AudioCodec.Opus(bitrate = 96))
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(
                mockAudioStream(codec = "aac", channels = 2, profile = AacProfile.LC.ffmpegName, disposition = mockDisposition(), tags = mockTags()),
                mockAudioStream(codec = "vorbis", channels = 2, profile = "", disposition = mockDisposition(), tags = mockTags())
            )
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a:1", "-c:a:1", "opus", "-b:a:1", "96k", "-application", "audio"
            ),
            args
        )
    }

    @Test
    @DisplayName("PCM input downmix to AAC stereo 192k")
    fun pcmInputDownmixToAacStereo() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Copy),
            audioTracks = mutableListOf(AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Aac(bitrate = 192, channels = 2)))
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "rawvideo", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "pcm_s16le", channels = 6, profile = "", disposition = mockDisposition(), tags = mockTags()))
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "192k", "-ac:0", "2"
            ),
            args
        )
    }

    @Test
    @DisplayName("FLAC input remux to FLAC (no reencode)")
    fun flacInputRemux() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Copy),
            audioTracks = mutableListOf(AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Flac()))
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(codec = "rawvideo", disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(mockAudioStream(codec = "flac", channels = 2, profile = "", disposition = mockDisposition(), tags = mockTags()))
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "copy"
            ),
            args
        )
    }

    @Test
    @DisplayName("Extended track skipped when same as default and default is copy")
    fun skipExtendedIfSameAsDefaultAndDefaultIsCopy() {
        val defaultTarget = AudioTarget(
            listIndex = 0,
            ffmpegIndex = 0,
            codec = AudioCodec.Copy
        )
        val extendedTarget = AudioTarget(
            listIndex = 0,
            ffmpegIndex = 0,
            codec = AudioCodec.Copy
        )

        val plan = MediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Copy),
            audioTracks = mutableListOf(defaultTarget, extendedTarget)
        )

        val audioStreams = listOf(
            mockAudioStream(codec = "aac", channels = 2, disposition = mockDisposition(), tags = mockTags())
        )
        val videoStreams = listOf(
            mockVideoStream(codec = "h264", disposition = mockDisposition(), tags = mockTags())
        )

        val args = plan.toFfmpegArgs(videoStreams, audioStreams)

        val expected = listOf(
            "-map", "0:v:0", "-c:v", "copy",
            "-map", "0:a:0", "-c:a:0", "copy"
        )
        assertEquals(expected, args)
    }

    @Test
    @DisplayName("Video=H264 + Audio=AAC → mp4")
    fun testChooseContainerMp4() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.H264()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Aac(channels = 2)))
        )
        assertEquals("mp4", plan.toContainer())
    }

    @Test
    @DisplayName("Video=VP9 + Audio=Opus → webm")
    fun testChooseContainerWebm() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Vp9()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Opus()))
        )
        assertEquals("webm", plan.toContainer())
    }

    @Test
    @DisplayName("Video=AV1 + Audio=FLAC → mkv")
    fun testChooseContainerMkv() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Av1()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Flac()))
        )
        assertEquals("mkv", plan.toContainer())
    }

    @Test
    @DisplayName("HEVC + AAC → mp4")
    fun testHevcWithAacGivesMp4() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Aac(channels = 2)))
        )
        assertEquals("mp4", plan.toContainer())
    }

    @Test
    @DisplayName("HEVC + AC3 → mkv")
    fun testHevcWithAc3GivesMkv() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Ac3()))
        )
        assertEquals("mkv", plan.toContainer())
    }

    @Test
    @DisplayName("HEVC + DTS → mkv")
    fun testHevcWithDtsGivesMkv() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Dts()))
        )
        assertEquals("mkv", plan.toContainer())
    }

    @Test
    @DisplayName("""
    Når videoTrack har ulik listIndex og ffmpegIndex
    Hvis toFfmpegArgs kalles
    Så:
        Skal listIndex brukes for å hente stream
        Og ffmpegIndex brukes i -map
""")
    fun testVideoListIndexVsFfmpegIndex() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(
                listIndex = 1,
                ffmpegIndex = 7,
                codec = VideoCodec.Copy
            ),
            audioTracks = mutableListOf()
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(
                mockVideoStream(
                    index = 10,
                    codec = "vp9",
                    disposition = mockDisposition(),
                    tags = mockTags()
                ),
                mockVideoStream(
                    index = 20,
                    codec = "h264",
                    disposition = mockDisposition(),
                    tags = mockTags()
                ) // listIndex = 1 → denne brukes
            ),
            audioStreams = emptyList()
        )

        assertEquals(
            listOf("-map", "0:v:7", "-c:v", "copy"),
            args
        )
    }

    @Test
    @DisplayName("""
    Når audioTrack har ulik listIndex og ffmpegIndex
    Hvis toFfmpegArgs kalles
    Så:
        Skal listIndex brukes for lookup
        Og ffmpegIndex brukes i -map
""")
    fun testAudioListIndexVsFfmpegIndex() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 1, ffmpegIndex = 5, codec = AudioCodec.Copy)
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(
                mockVideoStream(
                    disposition = mockDisposition(),
                    tags = mockTags()
                )
            ),
            audioStreams = listOf(
                mockAudioStream(
                    index = 11,
                    disposition = mockDisposition(),
                    tags = mockTags()
                ),
                mockAudioStream(
                    index = 22,
                    disposition = mockDisposition(),
                    tags = mockTags()
                ) // listIndex = 1 → denne brukes
            )
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:5", "-c:a:0", "copy"
            ),
            args
        )
    }

    @Test
    @DisplayName("""
    Når extended audioTrack mangler listIndex eller ffmpegIndex
    Hvis toFfmpegArgs kalles
    Så:
        Skal extended-sporet ignoreres
""")
    fun testExtendedSkippedWhenNullIndexes() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(0, 0, AudioCodec.Copy)
                // Ingen extended legges til → simulerer at listener filtrerte det bort
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(
                mockVideoStream(disposition = mockDisposition(), tags = mockTags())
            ),
            audioStreams = listOf(
                mockAudioStream(disposition = mockDisposition(), tags = mockTags())
            )
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "copy"
            ),
            args
        )
    }

    @Test
    @DisplayName("""
    Når default og extended peker på samme ffmpegIndex
    Hvis begge er copy
    Så:
        Skal extended-sporet ignoreres
""")
    fun testExtendedSkippedWhenSameIndexAndCopy() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(0, 0, AudioCodec.Copy),
                AudioTarget(0, 0, AudioCodec.Copy)
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(
                mockVideoStream(
                    disposition = mockDisposition(),
                    tags = mockTags()
                )
            ),
            audioStreams = listOf(
                mockAudioStream(
                    disposition = mockDisposition(),
                    tags = mockTags()
                )
            )
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "copy"
            ),
            args
        )
    }

    @Test
    @DisplayName("""
    Når default og extended peker på samme ffmpegIndex
    Hvis codec er forskjellig
    Så:
        Skal extended-sporet beholdes
""")
    fun testExtendedKeptWhenCodecDiffers() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(0, 0, AudioCodec.Copy),
                AudioTarget(0, 0, AudioCodec.Aac(bitrate = 128))
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(
                mockVideoStream(
                    disposition = mockDisposition(),
                    tags = mockTags()
                )
            ),
            audioStreams = listOf(
                mockAudioStream(
                    disposition = mockDisposition(),
                    tags = mockTags()
                )
            )
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:0", "-c:a:0", "copy",
                "-map", "0:a:0", "-c:a:1", "aac", "-b:a:1", "128k"
            ),
            args
        )
    }

    @Test
    @DisplayName("""
    Når flere audioTargets finnes
    Hvis toFfmpegArgs kalles
    Så:
        Skal output-indekser følge rekkefølgen i audioTracks-listen
""")
    fun testAudioOutputIndexOrder() {
        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(1, 5, AudioCodec.Aac(bitrate = 128)), // → -c:a:0
                AudioTarget(0, 2, AudioCodec.Opus(bitrate = 96))  // → -c:a:1
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(
                mockVideoStream(
                    disposition = mockDisposition(),
                    tags = mockTags()
                )
            ),
            audioStreams = listOf(
                mockAudioStream(
                    index = 2,
                    disposition = mockDisposition(),
                    tags = mockTags()
                ),
                mockAudioStream(
                    index = 5,
                    disposition = mockDisposition(),
                    tags = mockTags()
                )
            )
        )

        assertEquals(
            listOf(
                "-map", "0:v:0", "-c:v", "copy",
                "-map", "0:a:5", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a:2", "-c:a:1", "opus", "-b:a:1", "96k", "-application", "audio"
            ),
            args
        )
    }


    @Test
    @DisplayName("""
    Når codec ber om høyere bitrate, channels og samplerate enn kilden
    Hvis source har lavere verdier
    Så:
        Skal MediaPlan clamp'e alle verdier til source
""")
    fun testNoUpscalingBitrateChannelsSampleRate() {

        // Source: stereo, 48000 Hz, 128k bitrate
        val source = mockAudioStream(
            index = 0,
            codec = "aac",
            channels = 2,
            profile = "LC",
            disposition = mockDisposition(),
            tags = mockTags()
        ).copy(
            sample_rate = "48000",
            bit_rate = 128_000L
        )

        val plan = MediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(
                    listIndex = 0,
                    ffmpegIndex = 0,
                    codec = AudioCodec.Aac(
                        bitrate = 320,   // request 320k
                        channels = 8,    // request 7.1
                        sampleRate = 96000
                    )
                )
            )
        )

        val args = plan.toFfmpegArgs(
            videoStreams = listOf(mockVideoStream(disposition = mockDisposition(), tags = mockTags())),
            audioStreams = listOf(source)
        )

        // Rekkefølgen FFmpeg genererer er:
        // -c:a:0 aac
        // -b:a:0 128k
        // -ar:0 48000
        // -ac:0 2

        assertThat(args).containsSequence("-c:a:0", "aac")
        assertThat(args).containsSequence("-b:a:0", "128k")     // bitrate clamped
        assertThat(args).containsSequence("-ar:0", "48000")     // samplerate clamped
        assertThat(args).containsSequence("-ac:0", "2")         // channels clamped
    }





    // ------------------------------------------------------------
    // MOCK HELPERS
    // ------------------------------------------------------------

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
        bits_per_sample = 0,
        bit_rate = 48000
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