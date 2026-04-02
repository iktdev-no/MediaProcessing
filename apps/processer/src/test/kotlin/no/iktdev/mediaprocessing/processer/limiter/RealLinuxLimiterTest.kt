package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.Fs
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

@Tag("ci-only")
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class RealLinuxLimiterTest {


    @Test
    fun `can create cgroup and write cpu max`() {
        val limiter = LinuxCpuLimiterService(Fs())
        Assumptions.assumeTrue(
            limiter.supportsLimit(),
            "Skipping: CPU limiting not supported\n${limiter.supportReport()}"
        )

        // Start a harmless process
        val process = ProcessBuilder("sleep", "5").start()
        val pid = process.pid()

        try {
            limiter.limitProcess(pid, 20)

            val gPath = "/sys/fs/cgroup/mediaprocessing/ffmpeg-$pid"
            val cpuMax = File("$gPath/cpu.max")

            assertTrue(cpuMax.exists(), "cpu.max should exist after limitProcess")
            val content = cpuMax.readText().trim()

            assertTrue(
                content.contains("100000"),
                "cpu.max should contain a valid quota, got: $content"
            )

        } finally {
            limiter.removeLimit(pid)
            process.destroyForcibly()
            process.waitFor(1, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `process gets throttled by cgroup`() {
        val limiter = LinuxCpuLimiterService(no.iktdev.mediaprocessing.processer.services.fs.Fs())

        // 🔥 Start en CPU-heavy prosess (busy loop i bash)
        val process = ProcessBuilder(
            "bash",
            "-c",
            "while :; do :; done"
        )
            .redirectErrorStream(true)
            .start()

        val pid = process.pid()

        try {
            // Gi prosessen litt tid til å starte
            Thread.sleep(100)

            // 🔥 Apply hard limit (10%)
            limiter.limitProcess(pid, 10)

            // La den jobbe litt under throttling
            Thread.sleep(500)

            val statFile = File("/sys/fs/cgroup/mediaprocessing/ffmpeg-$pid/cpu.stat")

            assertTrue(statFile.exists(), "cpu.stat should exist")

            val stat = statFile.readText()

            // Eksempel:
            // nr_periods 123
            // nr_throttled 45
            // throttled_usec 123456
            val throttled = stat.lines()
                .firstOrNull { it.startsWith("nr_throttled") }
                ?.split(" ")
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: 0L

            assertTrue(
                throttled > 0,
                "Process should be throttled but nr_throttled=$throttled\n$stat"
            )

        } finally {
            // Cleanup uansett hva som skjer
            limiter.removeLimit(pid)

            process.destroy()
            process.waitFor(1, TimeUnit.SECONDS)
            process.destroyForcibly()
        }
    }
}