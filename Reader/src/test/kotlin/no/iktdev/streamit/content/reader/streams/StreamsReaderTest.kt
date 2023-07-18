package no.iktdev.streamit.content.reader.streams

import com.google.gson.Gson
import no.iktdev.streamit.content.reader.fileWatcher.FileWatcher
import no.iktdev.streamit.library.kafka.dto.Message
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StreamsReaderTest {

    @Test
    fun testDecode() {
        val data = """
            {
            	"referenceId": "7b332099-c663-4158-84d0-9972770316bb",
            	"status": {
            		"statusType": "SUCCESS"
            	},
            	"data": {
            		"file": "/src/input/[AAA] Iseleve - 13 [1080p HEVC][00000].mkv",
            		"title": "Iseleve",
            		"desiredNewName": "Iseleve - 13 "
            	}
            }
        """.trimIndent()
        assertDoesNotThrow {
            val message = Gson().fromJson(data, Message::class.java)
            val result = message.dataAs(FileWatcher.FileResult::class.java)
            assertThat(result?.title).isEqualTo("Iseleve")
        }
    }

}