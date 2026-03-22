package no.iktdev.mediaprocessing.ffmpeg.dsl.plan

import no.iktdev.mediaprocessing.ffmpeg.assertContainsAllWithOffset
import no.iktdev.mediaprocessing.ffmpeg.data.AudioStream
import no.iktdev.mediaprocessing.ffmpeg.data.Disposition
import no.iktdev.mediaprocessing.ffmpeg.data.Tags
import no.iktdev.mediaprocessing.ffmpeg.data.VideoStream
import no.iktdev.mediaprocessing.ffmpeg.dsl.AacProfile
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.Presets
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MediaPlanTest {

    @Test
    fun `video copy with one audio copy`() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Copy)
            )
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        Assertions.assertEquals(
            listOf(
                "-map", "0:v:0", "-map", "0:a:0", "-c:v:0", "copy", "-c:a:0", "copy"
            ),
            args.drop(5).dropLast(1)
        )
    }

    @DisplayName(
        """
        Når video skal reenkodes til HEVC med CRF
        Hvis input er H.264 og audio er MP3
        Så forventer vi at ffmpeg-argumentene inneholder korrekt mapping og bitrate
    """
    )
    @Test
    fun `video reencode to hevc with crf`() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Hevc(crf = 18)),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Aac(bitrate = 192))
            )
        )
        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg {
            fromInstructions(instruct)
        }.build()

        assertContainsAllWithOffset(expected =
            listOf(
                "-map", "0:v:0", "-c:v:0", "libx265", "-crf", "18", "-preset", "slow",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "192k"
            ),
            args, 5, 1)

    }

    @DisplayName(
        """
        Når to audio-spor skal transkodes med ulike codecs
        Hvis første spor skal til AAC 128k og andre til Opus 96k
        Så skal ffmpeg-argumentene reflektere korrekt mapping og bitrate
        """
    )
    @Test
    fun `two audio tracks with different codecs`() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(listIndex = 0, ffmpegIndex = 0, codec = VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Aac(bitrate = 128)),
                AudioTarget(listIndex = 1, ffmpegIndex = 1, codec = AudioCodec.Opus(bitrate = 96))
            )
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        assertContainsAllWithOffset(
            listOf(
                "-map", "0:v:0", "-c:v:0", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a:1", "-c:a:1", "opus", "-b:a:1", "96k", "-application", "audio"
            ),
            args, 5, 1
        )
    }


    @DisplayName(
        """
    Når video skal kopieres og audio skal reenkodes til AAC LC 128k
    Hvis input-sporet er AAC HE med 6 kanaler og kjent bitrate
    Så skal ffmpeg-argumentene bruke korrekt mapping og 128k bitrate
    """
    )
    @Test
    fun videoCopyAudioAacReencode() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(
                listIndex = 0,
                ffmpegIndex = 0,
                codec = VideoCodec.Copy
            ),
            audioTracks = mutableListOf(
                AudioTarget(
                    listIndex = 0,
                    ffmpegIndex = 1,
                    codec = AudioCodec.Aac(
                        bitrate = 128,
                        profile = AacProfile.LC
                    )
                )
            )
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        assertContainsAllWithOffset(
            listOf(
                "-map", "0:v:0", "-c:v:0", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k"
            ),
            args, 5, 1)

    }


    @Test
    @DisplayName("Video reencode to HEVC with CRF=18 and preset=slow, Audio copy")
    fun videoReencodeHevcCrfPresetAudioCopy() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(
                listIndex = 0,
                ffmpegIndex = 0,
                codec = VideoCodec.Hevc(crf = 18, preset = Presets.Slow)
            ),
            audioTracks = mutableListOf(AudioTarget(listIndex = 0, ffmpegIndex = 0, codec = AudioCodec.Copy))
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()


        assertContainsAllWithOffset(
            listOf(
                "-map", "0:v:0", "-c:v:0", "libx265", "-crf", "18", "-preset", "slow",
                "-map", "0:a:0", "-c:a:0", "copy"
            ),
            args, 5 ,1
        )
    }

    @DisplayName(
        """
    Når to audio-spor skal reenkodes med ulike codecs
    Hvis første spor skal til AAC 128k og andre til Opus 96k
    Så skal ffmpeg-argumentene bruke korrekt mapping og bitrates
    """
    )
    @Test
    fun twoAudioTracksDifferentCodecs() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(
                listIndex = 0,
                ffmpegIndex = 0,
                codec = VideoCodec.Copy
            ),
            audioTracks = mutableListOf(
                AudioTarget(
                    listIndex = 0,
                    ffmpegIndex = 0,
                    codec = AudioCodec.Aac(bitrate = 128)
                ),
                AudioTarget(
                    listIndex = 1,
                    ffmpegIndex = 1,
                    codec = AudioCodec.Opus(bitrate = 96)
                )
            )
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        assertContainsAllWithOffset(
            listOf(
                "-map", "0:v:0", "-c:v:0", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a:1", "-c:a:1", "opus", "-b:a:1", "96k", "-application", "audio"
            ),
            args.drop(5).dropLast(1)
        )
    }


    @Test
    @DisplayName("Video=H264 + Audio=AAC → mp4")
    fun testChooseContainerMp4() {
        val plan = BaseMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.H264()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Aac(channels = 2)))
        )
        Assertions.assertEquals("mp4", plan.toContainer())
    }

    @Test
    @DisplayName("Video=VP9 + Audio=Opus → webm")
    fun testChooseContainerWebm() {
        val plan = BaseMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Vp9()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Opus()))
        )
        Assertions.assertEquals("webm", plan.toContainer())
    }

    @Test
    @DisplayName("Video=AV1 + Audio=FLAC → mkv")
    fun testChooseContainerMkv() {
        val plan = BaseMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Av1()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Flac()))
        )
        Assertions.assertEquals("mkv", plan.toContainer())
    }

    @Test
    @DisplayName("HEVC + AAC → mp4")
    fun testHevcWithAacGivesMp4() {
        val plan = BaseMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Aac(channels = 2)))
        )
        Assertions.assertEquals("mp4", plan.toContainer())
    }

    @Test
    @DisplayName("HEVC + AC3 → mkv")
    fun testHevcWithAc3GivesMkv() {
        val plan = BaseMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Ac3()))
        )
        Assertions.assertEquals("mkv", plan.toContainer())
    }

    @Test
    @DisplayName("HEVC + DTS → mkv")
    fun testHevcWithDtsGivesMkv() {
        val plan = BaseMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc()),
            audioTracks = mutableListOf(AudioTarget(0, 0, AudioCodec.Dts()))
        )
        Assertions.assertEquals("mkv", plan.toContainer())
    }

    @Test
    @DisplayName(
        """
    Når videoTrack har ulik listIndex og ffmpegIndex
    Hvis toFfmpegArgs kalles
    Så:
        Skal listIndex brukes for å hente stream
        Og ffmpegIndex brukes i -map
"""
    )
    fun testVideoListIndexVsFfmpegIndex() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(
                listIndex = 1,
                ffmpegIndex = 7,
                codec = VideoCodec.Copy
            ),
            audioTracks = mutableListOf()
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        assertContainsAllWithOffset(
            listOf("-map", "0:v:1", "-c:v:0", "copy"),
            args, 5, 1
        )
    }

    @Test
    @DisplayName(
        """
    Når audioTrack har ulik listIndex og ffmpegIndex
    Hvis toFfmpegArgs kalles
    Så:
        Skal listIndex brukes for lookup
        Og ffmpegIndex brukes i -map
"""
    )
    fun testAudioListIndexVsFfmpegIndex() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(listIndex = 1, ffmpegIndex = 5, codec = AudioCodec.Copy)
            )
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        assertContainsAllWithOffset(
            listOf(
                "-map", "0:v:0", "-c:v:0", "copy",
                "-map", "0:a:1", "-c:a:0", "copy"
            ),
            args, 5, 1
        )
    }

    @Test
    @DisplayName(
        """
    Når flere audioTargets finnes
    Hvis toFfmpegArgs kalles
    Så:
        Skal output-indekser følge rekkefølgen i audioTracks-listen
    """
    )
    fun testAudioOutputIndexOrder() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = mutableListOf(
                AudioTarget(1, 5, AudioCodec.Aac(bitrate = 128)), // → -c:a:0
                AudioTarget(0, 2, AudioCodec.Opus(bitrate = 96))  // → -c:a:1
            )
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        assertContainsAllWithOffset(
            listOf(
                "-map", "0:v:0", "-c:v:0", "copy",
                "-map", "0:a:1", "-c:a:0", "aac", "-b:a:0", "128k",
                "-map", "0:a:0", "-c:a:1", "opus", "-b:a:1", "96k", "-application", "audio"
            ),
            args, 5 ,1
        )
    }

    @Test
    @DisplayName(
        """
            Når vi ber om å bygge argumenter for ffmpeg
            Hvis instruksjonene inneholdt mer enn 1 strømm/track
            Så:
              Skal vi bruke map og returnere 1 unik per track
    """
    )
    fun audioArgsReturnsNestedLists() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = listOf(
                AudioTarget(0, 0, AudioCodec.Aac(bitrate = 128)),
                AudioTarget(1, 1, AudioCodec.Copy)
            )
        )


        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        // Finn alle audio-map entries
        val audioMaps = args.windowed(2).filter { it[0] == "-map" && it[1].startsWith("0:a:") }

        // Vi forventer 2 audio-spor
        assertEquals(2, audioMaps.size)

        // Verifiser at mappingene er riktige
        assertThat(args).containsSequence("-map", "0:a:0")
        assertThat(args).containsSequence("-map", "0:a:1")
    }


    @Test
    @DisplayName(
        """
    Når LinearMediaPlan brukes
    Hvis video og audio genereres fra BaseMediaPlan
    Så:
        Skal createLinearFfmpegArguments kombinere begge i korrekt rekkefølge
    """
    )
    fun linearPlanCombinesVideoAndAudio() {
        val plan = LinearMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = listOf(
                AudioTarget(0, 0, AudioCodec.Aac(bitrate = 128))
            )
        )

        val instruct = plan.toInstructions("Mock.mkv", "Out.mp4")
        val args = ffmpeg { fromInstructions(instruct) }.build()

        assertContainsAllWithOffset(
            listOf(
                "-map", "0:v:0", "-c:v:0", "copy",
                "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "128k"
            ),
            args, 5 ,1
        )
    }

    @Test
    @DisplayName(
        """
        Når SegmentedMediaPlan brukes
        Hvis createSegmentedVideoArguments kalles
        Så:
            Skal ffmpeg-kommandoen kun inneholde video-relaterte argumenter
    """
    )
    fun segmentedPlanReturnsVideoOnly() {
        val plan = SegmentedMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc(crf = 18)),
            audioTracks = emptyList()
        )

        plan.toVideoInstructions("Mock.mkv", "Out.mp4").let {
            assertContainsAllWithOffset(listOf(
                "-map", "0:v:0", "-c:v:0", "libx265",  "-crf", "18", "-preset", "slow"
            ),
                ffmpeg { fromInstructions(it) }.build(), 5, 1)
        }
    }

    @Test
    @DisplayName(
        """
    Når SegmentedMediaPlan brukes
    Hvis createSegmentedAudioArguments kalles
    Så:
        Skal returverdien være en liste av lister (én per audio-spor)
    """
    )
    fun segmentedPlanReturnsAudioOnly() {
        val plan = SegmentedMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = listOf(
                AudioTarget(0, 0, AudioCodec.Aac(bitrate = 192))
            )
        )

        plan.toVideoInstructions("Mock.mkv", "Out.mp4").let {
            assertContainsAllWithOffset(listOf(
                "-map", "0:v:0", "-c:v:0", "copy"
            ),
                ffmpeg { fromInstructions(it) }.build(), 5, 1)
        }

        val audioInstructs = plan.toAudioInstructions("Mock.mkv")
        assertEquals(1, audioInstructs.size)
        audioInstructs.forEach { instruct ->
            val args = ffmpeg { fromInstructions(instruct) }.build()
            assertContainsAllWithOffset(
                listOf(
                    "-map", "0:a:0", "-c:a:0", "aac", "-b:a:0", "192k"
                ),
                args, 5, 1
            )
        }
    }


    // ------------------------------------------------------------
    // MOCK HELPERS
    // ------------------------------------------------------------




}