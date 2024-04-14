package no.iktdev.mediaprocessing.shared.common.parsing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class FileNameParserTest {

    @Test
    fun testParsing() {
        val file = File("/src/input/Fancy.Thomas.S03.1080p.AMAZING.WEB-VALUE.DDP5AN.1.H.264/Fancy.Thomas.S03E03.Enemy.1080p.AMAZING.WEB-VALUE.DDP5AN.1.H.264.mkv")
        val fnp = FileNameParser(file.nameWithoutExtension)
        assertThat(fnp.guessDesiredTitle()).isEqualTo("Fancy Thomas")
        assertThat(fnp.guessDesiredFileName()).isEqualTo("Fancy Thomas S03E03 Enemy")
    }

    @Test
    fun serieNameWithNumbers() {
        val name = "[TST] Fancy Name Test 99 - 01 [Nans][#00A8E6]"
        val parser = FileNameParser(name)
        val result = parser.guessDesiredTitle()
        assertThat(result).isEqualTo("Fancy Name Test 99")
        assertThat(parser.guessDesiredFileName()).isEqualTo("Fancy Name Test 99 - 01")
    }



}