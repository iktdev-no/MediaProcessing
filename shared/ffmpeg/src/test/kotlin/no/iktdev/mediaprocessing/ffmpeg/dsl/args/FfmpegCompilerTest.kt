package no.iktdev.mediaprocessing.ffmpeg.dsl.args

import no.iktdev.mediaprocessing.ffmpeg.TestBase
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.DefaultAsserter.fail

class FfmpegCompilerTest : TestBase() {

    private fun compile(block: FfmpegDsl.() -> Unit): List<String> {
        return ffmpeg(block).build()
    }

    @Test
    fun `video copy + one audio track`() {
        val args = compile {
            input("in.mkv") {
                video(0) { map = true; codec = VideoCodec.Copy }
                audio(0) { map = true; codec = AudioCodec.Aac(bitrate = 128) }
            }
            output("out.mp4") { overwrite = true }
        }

        assertContainsSequence(listOf("-map", "0:v:0"), args)
        assertContainsSequence(listOf("-map", "0:a:0"), args)
        assertContainsSequence(listOf("-c:v:0", "copy"), args)
        assertContainsSequence(listOf("-c:a:0", "aac"), args)
        assertContainsSequence(listOf("-b:a:0", "128k"), args)
        assertTrue(args.contains("-dn"))
    }

    @Test
    fun `two audio tracks in same input get unique output indexes`() {
        val args = compile {
            input("in.mkv") {
                audio(0) { map = true; codec = AudioCodec.Aac(bitrate = 128) }
                audio(1) { map = true; codec = AudioCodec.Opus(bitrate = 96) }
            }
            output("out.mp4") { overwrite = true }
        }

        assertContainsSequence(listOf("-map", "0:a:0"), args)
        assertContainsSequence(listOf("-map", "0:a:1"), args)

        assertContainsSequence(listOf("-c:a:0", "aac"), args)
        assertContainsSequence(listOf("-b:a:0", "128k"), args)

        assertContainsSequence(listOf("-c:a:1", "opus"), args)
        assertContainsSequence(listOf("-b:a:1", "96k"), args)
        assertContainsSequence(listOf("-application", "audio"), args)
    }

    @Test
    fun `audio tracks from different inputs get unique output indexes`() {
        val args = compile {
            input("v.mkv") {
                video(0) { map = true; codec = VideoCodec.Copy }
            }
            input("a1.mka") {
                audio(0) { map = true; codec = AudioCodec.Aac(bitrate = 128) }
            }
            input("a2.mka") {
                audio(0) { map = true; codec = AudioCodec.Opus(bitrate = 96) }
            }
            output("out.mp4") { overwrite = true }
        }

        assertContainsSequence(listOf("-map", "0:v:0"), args)
        assertContainsSequence(listOf("-map", "1:a:0"), args)
        assertContainsSequence(listOf("-map", "2:a:0"), args)

        assertContainsSequence(listOf("-c:a:0", "aac"), args)
        assertContainsSequence(listOf("-c:a:1", "opus"), args)
    }

    @Test
    fun `audio metadata is applied to correct output index`() {
        val args = compile {
            input("in.mkv") {
                audio(0) {
                    map = true
                    codec = AudioCodec.Aac(bitrate = 128)
                    language = "eng"
                    title = "English"
                    default = true
                }
                audio(1) {
                    map = true
                    codec = AudioCodec.Opus(bitrate = 96)
                    language = "jpn"
                    title = "Japanese"
                }
            }
            output("out.mp4") { overwrite = true }
        }

        assertContainsSequence(listOf("-metadata:s:a:0", "language=eng"), args)
        assertContainsSequence(listOf("-metadata:s:a:0", "title=English"), args)
        assertContainsSequence(listOf("-disposition:a:0", "default"), args)

        assertContainsSequence(listOf("-metadata:s:a:1", "language=jpn"), args)
        assertContainsSequence(listOf("-metadata:s:a:1", "title=Japanese"), args)
    }

    @Test
    fun `concat mode uses copy`() {
        val args = compile {
            concatFile("a.txt") {

            }
            output("out.mkv") { overwrite = true }
        }

        assertContainsSequence(listOf("-f", "concat"), args)
        assertContainsSequence(listOf("-c", "copy"), args)
    }

    @Test
    fun `output file is last argument`() {
        val args = compile {
            input("in.mkv") {
                video(0) { map = true; codec = VideoCodec.Copy }
            }
            output("out.mp4") { overwrite = true
            useWorkFile = false}
        }

        assertEquals("out.mp4", args.last())
    }

    @Test
    fun `dn flag is always present`() {
        val args = compile {
            input("in.mkv") {
                video(0) { map = true; codec = VideoCodec.Copy }
            }
            output("out.mp4") { overwrite = true }
        }

        assertTrue(args.contains("-dn"))
    }
}
