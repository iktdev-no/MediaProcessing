package no.iktdev.mediaprocessing.shared.common.parsing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow


class FileNameParserTest {

    @Test
    fun serieNameWithNumbers() {
        val name = "[TST] Fancy Name Test 99 - 01 [Nans][#00A8E6]"
        val parser = FileNameParser(name)
        val result = parser.guessDesiredTitle()
        assertThat(result).isEqualTo("Fancy Name Test 99")
        assertThat(parser.guessDesiredFileName()).isEqualTo("Fancy Name Test 99 - 01") }

}