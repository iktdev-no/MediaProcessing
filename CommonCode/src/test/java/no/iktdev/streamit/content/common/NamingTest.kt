package no.iktdev.streamit.content.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class NamingTest {

    @Test
    fun checkThatBracketsGetsRemoved() {
        val input = "[AAA] Sir fancy - 13 [1080p HEVC][000000]"
        val name = Naming(input)
        assertThat(name.guessDesiredTitle()).doesNotContain("[")

    }

/*
    @ParameterizedTest
    @MethodSource("serieOnlyTest")
    fun ensureOnlySerieAndDecodedCorrectly(testData: TestData) {
        val naming = Naming(testData.input).getName() ?: throw NullPointerException("Named is null")
        assertThat(naming.type).isEqualTo("serie")
        assertThat(naming.season).isEqualTo(testData.expected.season)
        assertThat(naming.episode).isEqualTo(testData.expected.episode)
    }

    @Test
    fun testTest() {
        val tmp = TestData(Naming.Name(title = "Demo", season = 1, episode = 1, type = "serie"), "[Kametsu] Ghost in the Shell Arise - 05 - Pyrophoric Cult (BD 1080p Hi10 FLAC) [13FF85A7]")
        val naming = Naming(tmp.input).getName()
        assertThat(naming).isNotNull()
    }


    fun serieOnlyTest(): List<Named<TestData>> {
        return listOf(
            Named.of("Is defined", TestData(Naming.Name(title = "Demo", season = 1, episode = 1, type = "serie"), "Demo - S01E01")),
            Named.of("Is decoded", TestData(Naming.Name("Demo!", "serie", season = 1, episode = 1), "[TMP] Demo! - 03")),
            Named.of("Is only Episode", TestData(Naming.Name("Demo", "serie", 1, 1), "Demo E1"))
        )
    }*/

/*
    data class TestData(
        val expected: Naming.Name,
        val input: String
    )*/
}