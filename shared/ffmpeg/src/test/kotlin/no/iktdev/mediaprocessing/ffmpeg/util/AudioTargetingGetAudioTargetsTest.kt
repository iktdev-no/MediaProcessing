package no.iktdev.mediaprocessing.ffmpeg.util

import no.iktdev.mediaprocessing.ffmpeg.MockData
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.model.SelectedAudioTracks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AudioTargetingGetAudioTargetsTest {

    private val mock = MockData()

    @Test
    @DisplayName(
        """
        Når kun default-spor er valgt
        Hvis getAudioTargets() kalles
        Så: returneres ett AudioTarget med korrekt index og clamping
        """
    )
    fun default_track_only() {
        val pref = AudioCodec.Aac(bitrate = 320, sampleRate = 48000, channels = 6)

        val stream = mock.mockAudioStream(
            index = 5,
            channels = 2,
            bitRate = 192_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(sample_rate = "44100")

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 5,
                extendedListIndex = null,
                extendedFfmpegIndex = null
            )
        )

        val targets = AudioTargeting(listOf(stream)).getAudioTargets(
            audioTracks = selected,
            preference = pref,
            extendedPreference = null,
        )

        assertEquals(1, targets.size)
        val t = targets.first()

        assertEquals(0, t.listIndex)
        assertEquals(5, t.ffmpegIndex)

        // clamped
        assertEquals(192, t.codec.bitrate)
        assertEquals(44100, t.codec.sampleRate)
        assertEquals(2, t.codec.channels)
    }

    @Test
    @DisplayName(
        """
        Når både default og extended finnes
        Hvis getAudioTargets() kalles
        Så: returneres to AudioTargets med korrekt mapping og clamping
        """
    )
    fun default_and_extended() {
        val defaultPref = AudioCodec.Aac(bitrate = 320, sampleRate = 48000, channels = 6)
        val extendedPref = AudioCodec.Aac(bitrate = 128, sampleRate = 44100, channels = 2)

        val stream0 = mock.mockAudioStream(
            index = 0,
            channels = 2,
            bitRate = 192_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(sample_rate = "44100")

        val stream1 = mock.mockAudioStream(
            index = 1,
            channels = 1,
            bitRate = 96_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(sample_rate = "22050")

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0,
                extendedListIndex = 1,
                extendedFfmpegIndex = 1
            )
        )

        val targets = AudioTargeting(listOf(stream0, stream1)).getAudioTargets(
            audioTracks = selected,
            preference = defaultPref,
            extendedPreference = extendedPref,
        )

        assertEquals(2, targets.size)

        val def = targets[0]
        val ext = targets[1]

        // mapping
        assertEquals(0, def.listIndex)
        assertEquals(0, def.ffmpegIndex)

        assertEquals(1, ext.listIndex)
        assertEquals(1, ext.ffmpegIndex)

        // clamping
        assertEquals(192, def.codec.bitrate)
        assertEquals(44100, def.codec.sampleRate)
        assertEquals(2, def.codec.channels)

        assertEquals(96, ext.codec.bitrate)
        assertEquals(22050, ext.codec.sampleRate)
        assertEquals(1, ext.codec.channels)
    }

    @Test
    @DisplayName(
        """
        Når extendedPreference ikke er satt
        Hvis getAudioTargets() kalles
        Så: extended bruker defaultPreference og clampes deretter
        """
    )
    fun extended_falls_back_to_default_preference() {
        val defaultPref = AudioCodec.Aac(bitrate = 256, sampleRate = 48000, channels = 6)

        val stream = mock.mockAudioStream(
            index = 1,
            channels = 2,
            bitRate = 128_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(sample_rate = "44100")

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0,
                extendedListIndex = 1,
                extendedFfmpegIndex = 1
            )
        )

        val targets = AudioTargeting(listOf(
            mock.mockAudioStream(
                index = 0,
                channels = 2,
                bitRate = 256_000,
                disposition = mock.mockDisposition(),
                tags = mock.mockTags()
            ),
            stream
        )).getAudioTargets(
            audioTracks = selected,
            preference = defaultPref,
            extendedPreference = null,
        )

        val ext = targets[1].codec

        // extended uses defaultPref → clamp to stream
        assertEquals(128, ext.bitrate)
        assertEquals(44100, ext.sampleRate)
        assertEquals(2, ext.channels)
    }
}
