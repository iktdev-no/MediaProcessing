package no.iktdev.mediaprocessing.processer.runners

import com.google.gson.JsonParser
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.iktdev.mediaprocessing.ffmpeg.data.FFinfoOutput
import no.iktdev.mediaprocessing.processer.TestBase
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProbeRunnerTest: TestBase() {

    private val testRoot = File("build/test-run")

    @BeforeEach
    fun clean() {
        if (testRoot.exists()) testRoot.deleteRecursively()
        testRoot.mkdirs()
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private fun ffinfoOutput(json: String, success: Boolean = true): FFinfoOutput {
        val parsed = JsonParser.parseString(json).asJsonObject
        return FFinfoOutput(
            success = success,
            data = parsed,
            error = null
        )
    }

    private fun ffinfoStreamsAndFormat(
        streams: String,
        format: String
    ): FFinfoOutput {
        val json = """
            {
              "streams": $streams,
              "format": $format
            }
        """.trimIndent()

        return ffinfoOutput(json)
    }

    private fun mockFFprobeReturning(result: FFinfoOutput) {
        mockkConstructor(ProbeRunner.JsonFfinfo::class)
        coEvery {
            anyConstructed<ProbeRunner.JsonFfinfo>().readJsonStreams(any())
        } returns result
    }

    // ---------------------------------------------------------
    // TEST 1 — Happy path
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når ffprobe returnerer gyldige streams og format
        Hvis ProbeRunner kjøres
        Så:
            Skal video- og audiostreams parses korrekt
    """)
    fun parses_valid_streams_and_format() = runTest {
        val file = workFolder.using("video.mp4")

        mockFFprobeReturning(
            ffinfoStreamsAndFormat(
                streams = """
                    [
                      { "codec_type": "video", "codec_name": "h264", "width": 1920, "height": 1080 },
                      { "codec_type": "audio", "codec_name": "aac", "sample_rate": "48000" }
                    ]
                """.trimIndent(),
                format = """
                    {
                      "duration": "120.0",
                      "format_name": "mov,mp4,m4a"
                    }
                """.trimIndent()
            )
        )

        val runner = ProbeRunner(file, "ffprobe")
        val result = runner.run()

        assertTrue(result is RunnerResult.Success)

        val payload = (result as RunnerResult.Success).payload

        assertEquals("120.0", payload.format.duration)
        assertEquals(1, payload.videoStreams.size)
        assertEquals(1, payload.audioStreams.size)

        assertEquals("h264", payload.videoStreams.first().codec_name)
        assertEquals("aac", payload.audioStreams.first().codec_name)
    }

    // ---------------------------------------------------------
    // TEST 2 — Missing streams
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når ffprobe mangler streams-feltet
        Hvis ProbeRunner kjøres
        Så:
            Skal RunnerResult.Reject returneres
    """)
    fun rejects_when_streams_missing() = runTest {
        val file = workFolder.using("video.mp4")

        mockFFprobeReturning(
            ffinfoOutput("""{ "format": {} }""")
        )

        val runner = ProbeRunner(file, "ffprobe")
        val result = runner.run()

        assertTrue(result is RunnerResult.Reject)

        val reason = (result as RunnerResult.Reject).reason

        assertEquals("Missing streams in ffprobe output", reason)
    }

    // ---------------------------------------------------------
    // TEST 3 — Missing format
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når ffprobe mangler format-feltet
        Hvis ProbeRunner kjøres
        Så:
            Skal RunnerResult.Reject returneres
    """)
    fun rejects_when_format_missing() = runTest {
        val file = workFolder.using("video.mp4")

        mockFFprobeReturning(
            ffinfoOutput("""{ "streams": [] }""")
        )

        val runner = ProbeRunner(file, "ffprobe")
        val result = runner.run()

        assertTrue(result is RunnerResult.Reject)

        val reason = (result as RunnerResult.Reject).reason

        assertEquals("Missing format in ffprobe output", reason)
    }

    // ---------------------------------------------------------
    // TEST 4 — Streams without codec_name are ignored
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når en stream mangler codec_name
        Hvis ProbeRunner kjøres
        Så:
            Skal streamen ignoreres
    """)
    fun ignores_streams_without_codec_name() = runTest {
        val file = workFolder.using("video.mp4")

        mockFFprobeReturning(
            ffinfoStreamsAndFormat(
                streams = """
                    [
                      { "codec_type": "video" },
                      { "codec_type": "audio", "codec_name": "aac" }
                    ]
                """.trimIndent(),
                format = """{ "duration": "10.0" }"""
            )
        )

        val runner = ProbeRunner(file, "ffprobe")
        val result = runner.run() as RunnerResult.Success

        assertEquals(0, result.payload.videoStreams.size)
        assertEquals(1, result.payload.audioStreams.size)
    }

    // ---------------------------------------------------------
    // TEST 5 — Unknown codec_type is ignored
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når en stream har ukjent codec_type
        Hvis ProbeRunner kjøres
        Så:
            Skal streamen ignoreres
    """)
    fun ignores_unknown_codec_type() = runTest {
        val file = workFolder.using("video.mp4")

        mockFFprobeReturning(
            ffinfoStreamsAndFormat(
                streams = """
                    [
                      { "codec_type": "subtitle", "codec_name": "subrip" }
                    ]
                """.trimIndent(),
                format = """{ "duration": "10.0" }"""
            )
        )

        val runner = ProbeRunner(file, "ffprobe")
        val result = runner.run() as RunnerResult.Success

        assertTrue(result.payload.videoStreams.isEmpty())
        assertTrue(result.payload.audioStreams.isEmpty())
    }
}
