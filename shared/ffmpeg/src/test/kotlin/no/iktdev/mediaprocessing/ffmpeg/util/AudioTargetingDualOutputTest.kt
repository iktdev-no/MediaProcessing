package no.iktdev.mediaprocessing.ffmpeg.util

import no.iktdev.mediaprocessing.ffmpeg.MockData
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.model.SelectedAudioTracks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class AudioTargetingDualOutputTest {

    private val mock = MockData()

    @Test
    @DisplayName(
        """
        Når en kilde har 5.1-lyd (EAC3/DTS)
        Hvis getAudioTargets() kalles med default + extended preferanser
        Så: returneres to AudioTargets (surround + stereo)
        """
    )
    fun one_source_becomes_two_targets() {
        // Source: 5.1 EAC3
        val stream = mock.mockAudioStream(
            index = 0,
            channels = 6,
            bitRate = 640_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(
            codec_name = "eac3",
            sample_rate = "48000"
        )

        // Default = stereo AAC
        val defaultPref = AudioCodec.Aac(
            bitrate = 192,
            sampleRate = 48000,
            channels = 2
        )

        // Extended = surround AAC (Chromecast støtter AAC 5.1)
        val extendedPref = AudioCodec.Aac(
            bitrate = 384,
            sampleRate = 48000,
            channels = 6
        )

        // Selected track: same source used for both default + extended
        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0,
                extendedListIndex = 0,
                extendedFfmpegIndex = 0
            )
        )

        val targets = AudioTargeting(listOf(stream)).getAudioTargets(
            audioTracks = selected,
            preference = defaultPref,
            extendedPreference = extendedPref,
        )

        assertEquals(2, targets.size)

        val stereo = targets[0]
        val surround = targets[1]

        // --- Verify mapping ---
        assertEquals(0, stereo.listIndex)
        assertEquals(0, stereo.ffmpegIndex)

        assertEquals(0, surround.listIndex)
        assertEquals(0, surround.ffmpegIndex)

        // --- Verify codec selection (NOT clamping, just type + channels) ---
        assertEquals(2, stereo.codec.channels)
        assertEquals(AudioCodec.Aac::class, stereo.codec::class)

        assertEquals(6, surround.codec.channels)
        assertEquals(AudioCodec.Aac::class, surround.codec::class)
    }

    @Test
    @DisplayName(
        """
    Når extendedPreference ikke er satt
    Hvis getAudioTargets() kalles
    Så: extended bruker defaultPreference og bygges som ekstra target
    """
    )
    fun extended_falls_back_to_default_preference() {
        val defaultPref = AudioCodec.Aac(
            bitrate = 256,
            sampleRate = 48000,
            channels = 6 // default har surround
        )

        val stream = mock.mockAudioStream(
            index = 0,
            channels = 6,
            bitRate = 640_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        )

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0,
                extendedListIndex = 0,
                extendedFfmpegIndex = 0
            )
        )

        val targets = AudioTargeting(listOf(stream)).getAudioTargets(
            audioTracks = selected,
            preference = defaultPref,
            extendedPreference = null,
        )

        assertEquals(2, targets.size)

        val (defaultTarget, extendedTarget) = targets

        // Default target
        assertEquals(6, defaultTarget.codec.channels)

        // Extended target uses fallback → also 6 channels
        assertEquals(6, extendedTarget.codec.channels)
    }

    @Test
    @DisplayName(
        """
    Når kilden har både 2.0 AAC og 5.1 DTS
    Hvis default peker på AAC og extended peker på DTS
    Så: default blir copy og extended blir 5.1 AAC
    """
    )
    fun two_sources_default_copy_extended_surround() {
        // --- Arrange ---
        val stereoAac = mock.mockAudioStream(
            index = 0,
            channels = 2,
            bitRate = 192_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(
            codec_name = "aac",
            sample_rate = "48000"
        )

        val dts51 = mock.mockAudioStream(
            index = 1,
            channels = 6,
            bitRate = 768_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(
            codec_name = "dts",
            sample_rate = "48000"
        )

        val streams = listOf(stereoAac, dts51)

        // Default = stereo AAC (copy)
        val defaultPref = AudioCodec.Aac(
            bitrate = null,      // null → no reencode
            sampleRate = null,
            channels = null
        )

        // Extended = 5.1 AAC
        val extendedPref = AudioCodec.Aac(
            bitrate = 384,
            sampleRate = 48000,
            channels = 6
        )

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0, // stereo AAC
                extendedListIndex = 1,
                extendedFfmpegIndex = 1  // DTS 5.1
            )
        )

        // --- Act ---
        val targets = AudioTargeting(streams).getAudioTargets(
            audioTracks = selected,
            preference = defaultPref,
            extendedPreference = extendedPref
        )

        // --- Assert ---
        assertEquals(2, targets.size)

        val defaultTarget = targets[0]
        val extendedTarget = targets[1]

        // Mapping
        assertEquals(0, defaultTarget.listIndex)
        assertEquals(0, defaultTarget.ffmpegIndex)

        assertEquals(1, extendedTarget.listIndex)
        assertEquals(1, extendedTarget.ffmpegIndex)

        // Default should remain AAC stereo (copy)
        assertEquals(AudioCodec.Aac::class, defaultTarget.codec::class)
        assertEquals(null, defaultTarget.codec.channels) // copy → no override

        // Extended should be AAC 5.1
        assertEquals(AudioCodec.Aac::class, extendedTarget.codec::class)
        assertEquals(6, extendedTarget.codec.channels)
    }


}
