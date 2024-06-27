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

    @Test
    fun serieName() {
        val inName = "Nihon.2024.S01E01.Gaijin.1080p.YT.HEVC"
        val parser = FileNameParser(inName)

        val title = parser.guessDesiredTitle()
        val result = parser.guessDesiredFileName()

        assertThat(title).isEqualTo("Nihon")
        assertThat(result).isEqualTo("Nihon S01E01 Gaijin")

    }

    @Test
    fun movieName() {
        val inName = "Wicket.Wicker.Potato.4.2023.UHD.BluRay.2160p"
        val parser = FileNameParser(inName)

        val title = parser.guessDesiredTitle()
        val result = parser.guessDesiredFileName()

        assertThat(title).isEqualTo("Wicket Wicker Potato 4")
        assertThat(result).isEqualTo("Wicket Wicker Potato 4")

    }

    @Test
    fun movieName2() {
        val inName = "Potato-Pass Movie - Skinke"
        val parser = FileNameParser(inName)

        val title = parser.guessDesiredTitle()
        val result = parser.guessDesiredFileName()

        assertThat(title).isEqualTo("Potato-Pass Movie")
        assertThat(result).isEqualTo("Potato-Pass Movie - Skinke")

    }

    @Test
    fun findTitleWithYear() {
        val input = "Dette er (en) tekst med (flere) paranteser som (potet) inneholder (år) som (2024) (2025).";
        val result = FileNameParser(input).guessSearchableTitle()
        assertThat(result).isEqualTo("Dette er tekst med paranteser som inneholder som (2024) (2025)")
    }

    @Test
    fun findSearchableTitle() {
        val input = "[FANCY] Urusei Yatsura (2022) - 36 [1080p HEVC]"
        val result = FileNameParser(input).guessSearchableTitle()
        assertThat(result.first()).isEqualTo("Urusei Yatsura (2022)")
    }

    @Test
    fun findSearchableTitle2() {
        val input = "[FANCY] Urusei Yatsura - 36 [1080p HEVC]"
        val result = FileNameParser(input).guessSearchableTitle()
        assertThat(result.first()).isEqualTo("Urusei Yatsura")
    }

    @Test
    fun testName() {
        val input = "The.Boys.S04E02.Life.Among.the.Septics.1080p.AMZN.WEB-DL.DDP5.1.H.264-NTb"
        val result = FileNameParser(input).guessSearchableTitle()
        assertThat(result.first()).isEqualTo("Urusei Yatsura (2022)")
    }

}