package no.iktdev.mediaprocessing.ffmpeg.util

import no.iktdev.mediaprocessing.ffmpeg.MockData
import no.iktdev.mediaprocessing.ffmpeg.TestBase
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.model.SelectedAudioTracks
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AudioTargetingTest: TestBase() {

    @Test
    @DisplayName("""
Når AudioTargeting får to EAC3 6ch strømmer
Hvis preference = AAC 2ch og extendedPreference = AAC 6ch
Så:
    Default target skal være AAC 2ch
    Extended target skal være AAC 6ch
""")
    fun selects_correct_audio_targets() {
        val mock = MockData()

        // --- GIVEN ---
        val streams = listOf(
            mock.mockAudioStream(
                index = 0,
                codec = "eac3",
                channels = 6,
                bitRate = 640_000,
                disposition = mock.mockDisposition(),
                tags = mock.mockTags()
            ),
            mock.mockAudioStream(
                index = 1,
                codec = "eac3",
                channels = 6,
                bitRate = 640_000,
                disposition = mock.mockDisposition(),
                tags = mock.mockTags()
            )
        )

        val targeting = AudioTargeting(streams)

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0,
                extendedListIndex = 1,
                extendedFfmpegIndex = 1
            )
        )

        val preference = AudioCodec.Aac().apply {
            channels = 2
            bitrate = 128
            sampleRate = 48000
        }

        val extendedPreference = AudioCodec.Aac().apply {
            channels = 6
            bitrate = 384
            sampleRate = 48000
        }

        // --- WHEN ---
        val targets = targeting.getAudioTargets(
            audioTracks = selected,
            preference = preference,
            extendedPreference = extendedPreference
        )

        // --- THEN ---
        assertEquals(2, targets.size)

        val default = targets[0]
        val extended = targets[1]

        // Default target → AAC 2ch
        assertEquals(0, default.listIndex)
        assertEquals(0, default.ffmpegIndex)
        assertEquals(2, default.codec.channels)
        assertEquals(128, default.codec.bitrate)
        assertEquals(48000, default.codec.sampleRate)

        // Extended target → AAC 6ch
        assertEquals(1, extended.listIndex)
        assertEquals(1, extended.ffmpegIndex)
        assertEquals(6, extended.codec.channels)
        assertEquals(384, extended.codec.bitrate)
        assertEquals(48000, extended.codec.sampleRate)
    }

}