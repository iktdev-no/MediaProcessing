package no.iktdev.mediaprocessing.shared.common.parsing

import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.EpisodeInfo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FileNameDeterminateTest {

    @Test
    fun testThatCorrectNumberIsSelected() {
        val title = "Fancy Name Test 99"
        val baseName  = "Fancy Name Test 99 - 01"
        val fnd = FileNameDeterminate(title, baseName, FileNameDeterminate.ContentType.UNDEFINED)
        val result = fnd.getDeterminedVideoInfo()
        assertNotNull(result)
        assertThat(result!!.type).isEqualTo("serie")
        assertThat(result).isInstanceOf(EpisodeInfo::class.java)
        val ei = result as EpisodeInfo
        assertThat(ei.episode).isEqualTo(1)
    }

}