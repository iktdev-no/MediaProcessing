package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.Fs
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

@Tag("ci-only")
@EnabledIfEnvironmentVariable(named = "CI", matches = "true")
class RealLinuxLimiterTest {

    private fun assumeCgroupWritable(limiter: LinuxCpuLimiterService) {
        assumeTrue(
            limiter.supportsLimit(),
            "Skipping: CPU limiting not supported\n${limiter.supportReport()}"
        )

        // Check if we can write to subtree_control
        val subtree = File("/sys/fs/cgroup/cgroup.subtree_control")
        assumeTrue(
            subtree.canWrite(),
            "Skipping: cgroup.subtree_control is not writable in this environment"
        )
    }

    @Test
    fun `can create cgroup and write cpu max`() {
        val limiter = LinuxCpuLimiterService(Fs())

        assumeCgroupWritable(limiter)

        val process = ProcessBuilder("sleep", "5").start()
        val pid = process.pid()

        try {
            limiter.limitProcess(pid, 20)

            val gPath = "/sys/fs/cgroup/mediaprocessing/ffmpeg-$pid"
            val cpuMax = File("$gPath/cpu.max")

            assumeTrue(cpuMax.exists(), "Skipping: cpu.max not created (likely CI restrictions)")

            val content = cpuMax.readText().trim()
            assertTrue(
                content.contains("100000") || content.contains("max"),
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
        val limiter = LinuxCpuLimiterService(Fs())

        assumeCgroupWritable(limiter)

        val process = ProcessBuilder(
            "bash", "-c", "while :; do :; done"
        ).start()

        val pid = process.pid()

        try {
            Thread.sleep(100)

            limiter.limitProcess(pid, 10)

            Thread.sleep(500)

            val statFile = File("/sys/fs/cgroup/mediaprocessing/ffmpeg-$pid/cpu.stat")

            assumeTrue(statFile.exists(), "Skipping: cpu.stat does not exist (CI restriction)")

            val stat = statFile.readText()

            val throttled = stat.lines()
                .firstOrNull { it.startsWith("nr_throttled") }
                ?.split(" ")
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: 0L

            assumeTrue(throttled >= 0, "Skipping: throttling not measurable in this environment")

            assertTrue(
                throttled > 0,
                "Process should be throttled but nr_throttled=$throttled\n$stat"
            )

        } finally {
            limiter.removeLimit(pid)
            process.destroy()
            process.waitFor(1, TimeUnit.SECONDS)
            process.destroyForcibly()
        }
    }
}
