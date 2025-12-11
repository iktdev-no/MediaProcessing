package no.iktdev.mediaprocessing.ffmpeg.decoder

import no.iktdev.mediaprocessing.ffmpeg.Files
import no.iktdev.mediaprocessing.ffmpeg.getAsList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FfmpegProgressDecoderTest {

    @Test
    @DisplayName("Verify that progress can be decoded")
    fun parseReadout1() {
        val lines = Files.Output1.getAsList()
        val decoder = FfmpegProgressDecoder()
        lines.forEach {
            decoder.defineDuration(it)
        }
        val result = decoder.parseVideoProgress(lines)
        assertThat(result?.progress).isNotNull()
        val progress = decoder.getProgress(result!!)
        assertThat(progress.progress).isGreaterThanOrEqualTo(0)
    }
}