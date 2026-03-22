package no.iktdev.mediaprocessing.ffmpeg.util

import no.iktdev.mediaprocessing.ffmpeg.MockData
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.model.SelectedAudioTracks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AudioTargetingClampTest {

    private val mock = MockData()

    @Test
    @DisplayName(
        """
        Når preferanse har høyere verdier enn source
        Hvis AudioTargeting bygges
        Så: clampes codec ned til source
        """
    )
    fun clamp_down_to_source() {
        val pref = AudioCodec.Aac(
            bitrate = 320,
            sampleRate = 48000,
            channels = 6
        )

        val stream = mock.mockAudioStream(
            index = 0,
            channels = 2,
            bitRate = 192_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(sample_rate = "44100")

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0,
                extendedListIndex = null,
                extendedFfmpegIndex = null
            )
        )

        val codec = AudioTargeting(listOf(stream))
            .getAudioTargets(selected, pref, null, )
            .first()
            .codec

        assertEquals(192, codec.bitrate)
        assertEquals(44100, codec.sampleRate)
        assertEquals(2, codec.channels)
    }

    @Test
    @DisplayName(
        """
        Når preferanse matcher source
        Hvis AudioTargeting bygges
        Så: beholdes verdiene (ingen clamp)
        """
    )
    fun equal_values_are_preserved() {
        val pref = AudioCodec.Aac(
            bitrate = 192,
            sampleRate = 44100,
            channels = 2
        )

        val stream = mock.mockAudioStream(
            index = 0,
            channels = 2,
            bitRate = 192_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(sample_rate = "44100")

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0,
                extendedListIndex = null,
                extendedFfmpegIndex = null
            )
        )

        val codec = AudioTargeting(listOf(stream))
            .getAudioTargets(selected, pref, null)
            .first()
            .codec

        assertEquals(192, codec.bitrate)
        assertEquals(44100, codec.sampleRate)
        assertEquals(2, codec.channels)
    }

    @Test
    @DisplayName(
        """
        Når preferanse er lavere enn source
        Hvis AudioTargeting bygges
        Så: beholdes preferansen (vi drar ikke opp verdier)
        """
    )
    fun lower_preference_is_not_raised() {
        val pref = AudioCodec.Aac(
            bitrate = 96,
            sampleRate = 22050,
            channels = 1
        )

        val stream = mock.mockAudioStream(
            index = 0,
            channels = 2,
            bitRate = 192_000,
            disposition = mock.mockDisposition(),
            tags = mock.mockTags()
        ).copy(sample_rate = "48000")

        val selected = listOf(
            SelectedAudioTracks(
                defaultListIndex = 0,
                defaultFfmpegIndex = 0,
                extendedListIndex = null,
                extendedFfmpegIndex = null
            )
        )

        val codec = AudioTargeting(listOf(stream))
            .getAudioTargets(selected, pref, null)
            .first()
            .codec

        assertEquals(96, codec.bitrate)
        assertEquals(22050, codec.sampleRate)
        assertEquals(1, codec.channels)
    }

    @Test
    @DisplayName(
        """
        Når både default og extended finnes
        Hvis AudioTargeting bygges
        Så: clampes begge uavhengig
        """
    )
    fun extended_and_default_clamp_independently() {
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

        val targets = AudioTargeting(listOf(stream0, stream1))
            .getAudioTargets(selected, defaultPref, extendedPref)

        val defaultCodec = targets[0].codec
        val extendedCodec = targets[1].codec

        assertEquals(192, defaultCodec.bitrate)
        assertEquals(44100, defaultCodec.sampleRate)
        assertEquals(2, defaultCodec.channels)

        assertEquals(96, extendedCodec.bitrate)
        assertEquals(22050, extendedCodec.sampleRate)
        assertEquals(1, extendedCodec.channels)
    }
}
