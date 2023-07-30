package no.iktdev.streamit.content.encode.progress

import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.encode.Resources
import no.iktdev.streamit.content.encode.runner.EncodeDaemon
import no.iktdev.streamit.content.encode.runner.IEncodeListener
import org.assertj.core.api.Assertions.assertThat
import org.junit.BeforeClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
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



    @Test
    fun testCanRead() {
        val res = Resources()
        val data = res.getText("Output1.txt") ?: ""
        assertThat(data).isNotEmpty()
        val lines = data.split("\n").map { it.trim() }
        assertThat(lines).isNotEmpty()

        val encodeWork = EncodeWork(
            workId = UUID.randomUUID().toString(),
            collection = "Demo",
            inFile = "Demo.mkv",
            outFile = "FancyDemo.mp4",
            arguments = emptyList()
        )
        val decoder = ProgressDecoder(encodeWork)
        lines.forEach { decoder.setDuration(it) }
        assertThat(decoder.duration).isNotNull()
        val produced = mutableListOf<Progress>()

        val tempFile = File.createTempFile("test", ".log")

        val encoder = EncodeDaemon(UUID.randomUUID().toString(), encodeWork, object : IEncodeListener {
            override fun onStarted(referenceId: String, work: EncodeWork) {
            }
            override fun onError(referenceId: String, work: EncodeWork, code: Int) {
            }
            override fun onProgress(referenceId: String, work: EncodeWork, progress: Progress) {
                produced.add(progress)
            }
            override fun onEnded(referenceId: String, work: EncodeWork) {
            }

        }, tempFile)


        lines.forEach {
            encoder.onOutputChanged(it)
        }
        assertThat(produced).isNotEmpty()


    }


    @Test
    fun testThatProgressIsCalculated() {
        val encodeWork = EncodeWork(
            workId = UUID.randomUUID().toString(),
            collection = "Demo",
            inFile = "Demo.mkv",
            outFile = "FancyDemo.mp4",
            arguments = emptyList()
        )
        val decoder = ProgressDecoder(encodeWork)
        decoder.setDuration("Duration: 01:48:54.82,")
        assertThat(decoder.duration).isNotNull()
        val decodedProgressData = DecodedProgressData(
            frame = null,
            fps = null,
            stream_0_0_q = null,
            bitrate = null,
            total_size = null,
            out_time_ms = null,
            out_time_us = null,
            out_time = "01:48:54.82",
            dup_frames = null,
            drop_frames = null,
            speed = 1.0,
            progress = "Continue"
        )
        val progress = decoder.getProgress(decodedProgressData)
        assertThat(progress.progress).isGreaterThanOrEqualTo(99)
    }

    @Test
    fun testThatProgressIsNotNone() {
        val encodeWork = EncodeWork(
            workId = UUID.randomUUID().toString(),
            collection = "Demo",
            inFile = "Demo.mkv",
            outFile = "FancyDemo.mp4",
            arguments = emptyList()
        )
        val decoder = ProgressDecoder(encodeWork)
        decoder.setDuration("Duration: 01:48:54.82,")
        assertThat(decoder.duration).isNotNull()
        val decodedProgressData = DecodedProgressData(
            frame = null,
            fps = null,
            stream_0_0_q = null,
            bitrate = null,
            total_size = null,
            out_time_ms = null,
            out_time_us = null,
            out_time = "01:00:50.174667",
            dup_frames = null,
            drop_frames = null,
            speed = 1.0,
            progress = "Continue"
        )
        val progress = decoder.getProgress(decodedProgressData)
        assertThat(progress.progress).isGreaterThanOrEqualTo(1)
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