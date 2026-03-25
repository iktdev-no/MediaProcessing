package no.iktdev.mediaprocessing.ffmpeg.dsl.plan

import no.iktdev.mediaprocessing.ffmpeg.assertContainsAllWithOffset
import no.iktdev.mediaprocessing.ffmpeg.dsl.AacProfile
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.Presets
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.ffmpeg
import no.iktdev.mediaprocessing.ffmpeg.model.AudioTarget
import no.iktdev.mediaprocessing.ffmpeg.model.VideoTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MediaPlanTest {

    private val defaultOffset = 6

    // -------------------------------------------------------------
    // VIDEO COPY + AUDIO COPY
    // -------------------------------------------------------------
    @Test
    fun `video copy with one audio copy`() {
        val plan = SimpleMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = listOf(AudioTarget(0, 0, AudioCodec.Copy()))
        )

        val video = plan.toVideoInstructions("Mock.mkv", "video.mkv")
        val audio = plan.toAudioInstructions("Mock.mkv")

        // VIDEO
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:v:0", "-c:v:0", "copy"),
            ffmpeg { fromInstructions(video) }.build(),
            defaultOffset, 1
        )

        // AUDIO
        val a0 = ffmpeg { fromInstructions(audio[0]) }.build()
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:a:0", "-c:a:0", "copy",
                "-metadata:s:a:0", "handler_name=Audio -1ch",
                "-disposition:a:0", "default"),
            a0, defaultOffset, 1
        )
    }

    // -------------------------------------------------------------
    // VIDEO REENCODE HEVC + AUDIO AAC
    // -------------------------------------------------------------
    @DisplayName(
        """
        Når video skal reenkodes til HEVC med CRF
        Hvis input er H.264 og audio er MP3
        Så forventer vi at ffmpeg-argumentene inneholder korrekt mapping og bitrate
        """
    )
    @Test
    fun `video reencode to hevc with crf`() {
        val plan = SimpleMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc(crf = 18)),
            audioTracks = listOf(AudioTarget(0, 0, AudioCodec.Aac(bitrate = 192, channels = 2)))
        )

        val video = plan.toVideoInstructions("Mock.mkv", "video.mkv")
        val audio = plan.toAudioInstructions("Mock.mkv")

        // VIDEO
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:v:0",
                "-c:v:0", "libx265", "-crf", "18", "-preset", "slow"),
            ffmpeg { fromInstructions(video) }.build(),
            defaultOffset, 1
        )

        // AUDIO
        val a0 = ffmpeg { fromInstructions(audio[0]) }.build()
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:a:0",
                "-c:a:0", "aac", "-b:a:0", "192k", "-ac:0", "2",
                "-metadata:s:a:0", "handler_name=Audio 2ch",
                "-disposition:a:0", "default"),
            a0, defaultOffset, 1
        )
    }

    // -------------------------------------------------------------
    // TO AUDIO-SPOR MED ULIKE CODECS
    // -------------------------------------------------------------
    @DisplayName(
        """
        Når to audio-spor skal transkodes med ulike codecs
        Hvis første spor skal til AAC 128k og andre til Opus 96k
        Så skal ffmpeg-argumentene reflektere korrekt mapping og bitrate
        """
    )
    @Test
    fun `two audio tracks with different codecs`() {
        val plan = SimpleMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = listOf(
                AudioTarget(0, 0, AudioCodec.Aac(bitrate = 128, channels = 2)),
                AudioTarget(1, 1, AudioCodec.Opus(bitrate = 96, channels = 2))
            )
        )

        val audio = plan.toAudioInstructions("Mock.mkv")
        assertEquals(2, audio.size)

        // AAC
        val aac = ffmpeg { fromInstructions(audio[0]) }.build()
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:a:0",
                "-c:a:0", "aac", "-b:a:0", "128k", "-ac:0", "2",
                "-metadata:s:a:0", "handler_name=Audio 2ch",
                "-disposition:a:0", "default"),
            aac, defaultOffset, 1
        )

        // OPUS
        val opus = ffmpeg { fromInstructions(audio[1]) }.build()
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:a:1",
                "-c:a:0", "opus", "-b:a:0", "96k", "-ac:0", "2", "-application", "audio",
                "-metadata:s:a:0", "handler_name=Audio 2ch",
                "-disposition:a:0", "default"),
            opus, defaultOffset, 1
        )
    }

    // -------------------------------------------------------------
    // VIDEO COPY + AUDIO AAC REENCODE
    // -------------------------------------------------------------
    @DisplayName(
        """
        Når video skal kopieres og audio skal reenkodes til AAC LC 128k
        Hvis input-sporet er AAC HE med 6 kanaler og kjent bitrate
        Så skal ffmpeg-argumentene bruke korrekt mapping og 128k bitrate
        """
    )
    @Test
    fun videoCopyAudioAacReencode() {
        val plan = SimpleMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = listOf(
                AudioTarget(
                    listIndex = 0,
                    ffmpegIndex = 1,
                    codec = AudioCodec.Aac(
                        bitrate = 128,
                        profile = AacProfile.LC,
                        channels = 2
                    )
                )
            )
        )

        val audio = plan.toAudioInstructions("Mock.mkv")
        val a0 = ffmpeg { fromInstructions(audio[0]) }.build()

        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:a:0",
                "-c:a:0", "aac", "-b:a:0", "128k", "-ac:0", "2",
                "-metadata:s:a:0", "handler_name=Audio 2ch",
                "-disposition:a:0", "default"),
            a0, defaultOffset, 1
        )
    }

    // -------------------------------------------------------------
    // VIDEO REENCODE HEVC + AUDIO COPY
    // -------------------------------------------------------------
    @Test
    @DisplayName("Video reencode to HEVC with CRF=18 and preset=slow, Audio copy")
    fun videoReencodeHevcCrfPresetAudioCopy() {
        val plan = SimpleMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Hevc(crf = 18, preset = Presets.Slow)),
            audioTracks = listOf(AudioTarget(0, 0, AudioCodec.Copy()))
        )

        val video = plan.toVideoInstructions("Mock.mkv", "video.mkv")
        val audio = plan.toAudioInstructions("Mock.mkv")

        // VIDEO
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:v:0",
                "-c:v:0", "libx265", "-crf", "18", "-preset", "slow"),
            ffmpeg { fromInstructions(video) }.build(),
            defaultOffset, 1
        )

        // AUDIO
        val a0 = ffmpeg { fromInstructions(audio[0]) }.build()
        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:a:0", "-c:a:0", "copy",
                "-metadata:s:a:0", "handler_name=Audio -1ch",
                "-disposition:a:0", "default"),
            a0, defaultOffset, 1
        )
    }

    // -------------------------------------------------------------
    // AAC 2CH + AAC 6CH → TO UNIKE SPOR
    // -------------------------------------------------------------
    @Test
    @DisplayName(
        """
        Når default og extended audio har samme codec-type (AAC)
        Hvis default er 2ch og extended er 6ch
        Så:
            Skal begge tracks anses som unike
            Og toAudioInstructions skal returnere 2 instruksjoner
        """
    )
    fun `aac default 2ch and extended 6ch produce two unique audio tracks`() {
        val plan = SimpleMediaPlan(
            videoTrack = VideoTarget(0, 0, VideoCodec.Copy),
            audioTracks = listOf(
                AudioTarget(0, 2, AudioCodec.Aac(channels = 2, bitrate = 128)),
                AudioTarget(0, 2, AudioCodec.Aac(channels = 6, bitrate = 384))
            )
        )

        val audio = plan.toAudioInstructions("Mock.mkv")
        assertEquals(2, audio.size)

        val a2 = ffmpeg { fromInstructions(audio[0]) }.build()
        val a6 = ffmpeg { fromInstructions(audio[1]) }.build()

        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:a:0",
                "-c:a:0", "aac", "-b:a:0", "128k", "-ac:0", "2",
                "-metadata:s:a:0", "handler_name=Audio 2ch",
                "-disposition:a:0", "default"),
            a2, defaultOffset, 1
        )

        assertContainsAllWithOffset(
            listOf("-map_chapters", "-1",
                "-map", "0:a:0",
                "-c:a:0", "aac", "-b:a:0", "384k", "-ac:0", "6",
                "-metadata:s:a:0", "handler_name=Audio 6ch",
                "-disposition:a:0", "default"),
            a6, defaultOffset, 1
        )
    }
}
