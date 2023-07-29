package no.iktdev.streamit.content.encode.progress

import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.UUID

class DecodedProgressDataDecoderTest {

    @Test
    fun test() {
        val progress = ProgressDecoder(EncodeWork(
            workId = UUID.randomUUID().toString(),
            collection = "Demo",
            inFile = "Demo.mkv",
            outFile = "FancyDemo.mp4",
            arguments = emptyList()
        ))
        val lines = text.split("\n")
        val cache: MutableList<String> = mutableListOf()
        lines.forEach {
            cache.add(it)
            assertDoesNotThrow {
                val progressItem =  progress.parseVideoProgress(cache)
                progressItem?.progress
            }
        }
        assertThat(lines).isNotEmpty()
    }

    val text = """
        frame=16811 fps= 88 q=40.0 size=    9984kB time=00:x01:10.79 bitrate=1155.3kbits/s speed=3.71x
        fps=88.03
        stream_0_0_q=40.0
        bitrate=1155.3kbits/s
        total_size=10223752
        out_time_us=70798005
        out_time_ms=70798005
        out_time=00:01:10.798005
        dup_frames=0
        drop_frames=0
        speed=3.71x
        progress=continue
        frame= 1710 fps= 84 q=-1.0 Lsize=   12124kB time=00:01:11.91 bitrate=1381.2kbits/s speed=3.53x
        frame=1710
        fps=84.01
        stream_0_0_q=-1.0
        bitrate=1381.2kbits/s
        total_size=12415473
        out_time_us=71910998
        out_time_ms=71910998
        out_time=00:01:11.910998
        dup_frames=0
        drop_frames=0
        speed=3.53x
        progress=end
    """.trimIndent()
}