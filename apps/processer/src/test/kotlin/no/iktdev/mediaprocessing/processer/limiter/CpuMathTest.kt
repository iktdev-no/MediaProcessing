package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CpuMathTest {

    private fun limiter(fs: FakeFs = FakeFs()) =
        LinuxCpuLimiterService(fs)

    private fun setupGroup(fs: FakeFs, pid: Long = 1234): String {
        val gPath = "/cgroup/ffmpeg-$pid"
        fs.mkdirs(gPath)
        return gPath
    }

    // -----------------------------
    // cpuQuota tests
    // -----------------------------

    @Test
    fun `cpuQuota clamps to minimum`() {
        val fs = FakeFs()
        val l = limiter(fs)
        val gPath = setupGroup(fs)

        fs.writeText("$gPath/cpu.max", "100000 100000")

        val q = l.cpuQuota(gPath, 1)
        val quota = q.split(" ")[0].toLong()

        assertTrue(quota >= 1000)
    }

    @Test
    fun `cpuQuota scales correctly with percent`() {
        val fs = FakeFs()
        val l = limiter(fs)
        val gPath = setupGroup(fs)

        fs.writeText("$gPath/cpu.max", "100000 100000")

        val q10 = l.cpuQuota(gPath, 10).split(" ")[0].toLong()
        val q50 = l.cpuQuota(gPath, 50).split(" ")[0].toLong()
        val q100 = l.cpuQuota(gPath, 100).split(" ")[0].toLong()

        assertTrue(q10 < q50)
        assertTrue(q50 < q100)
    }

    @Test
    fun `cpuQuota returns max when cpu max is max`() {
        val fs = FakeFs()
        val l = limiter(fs)
        val gPath = setupGroup(fs)

        fs.writeText("$gPath/cpu.max", "max 100000")

        val q = l.cpuQuota(gPath, 50)
        assertEquals("max", q)
    }

    // -----------------------------
    // cpuCount tests
    // -----------------------------

    @Test
    fun `cpuCount returns physical cpu count when cpuset missing`() {
        val fs = FakeFs()
        val l = limiter(fs)

        assertEquals(
            Runtime.getRuntime().availableProcessors(),
            l.cpuCount()
        )
    }

    @Test
    fun `cpuCount uses cpuset when available`() {
        val fs = FakeFs()
        fs.mkdirs("/cgroup")
        fs.writeText("/cgroup/cpuset.cpus.effective", "0-1")

        val l = limiter(fs)

        assertEquals(2, l.cpuCount())
    }

    @Test
    fun `cpuCount handles invalid cpuset gracefully`() {
        val fs = FakeFs()
        fs.mkdirs("/cgroup")
        fs.writeText("/cgroup/cpuset.cpus.effective", "invalid")

        val l = limiter(fs)

        assertEquals(
            Runtime.getRuntime().availableProcessors(),
            l.cpuCount()
        )
    }
}
