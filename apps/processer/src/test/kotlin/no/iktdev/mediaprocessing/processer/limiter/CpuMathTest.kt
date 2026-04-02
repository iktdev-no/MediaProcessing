package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CpuMathTest {

    private fun limiter(fs: FakeFs = FakeFs()) =
        LinuxCpuLimiterService(fs)

    @Test
    fun `cpuQuota clamps to minimum`() {
        val l = limiter()
        val q = l.cpuQuotaForTest(1)
        val quota = q.split(" ")[0].toLong()
        assertTrue(quota >= 1000)
    }

    @Test
    fun `cpuCount uses quota when available`() {
        val fs = FakeFs()
        fs.mkdirs("/sys/fs/cgroup")
        fs.writeText("/sys/fs/cgroup/cpu.max", "20000 100000")

        val l = limiter(fs)
        assertEquals(1, l.cpuCount())
    }

    @Test
    fun `cpuCount falls back to availableProcessors`() {
        val l = limiter()
        assertEquals(Runtime.getRuntime().availableProcessors(), l.cpuCount())
    }

    @Test
    fun `cpuQuota scales correctly with percent`() {
        val l = limiter()

        val q10 = l.cpuQuotaForTest(10).split(" ")[0].toLong()
        val q50 = l.cpuQuotaForTest(50).split(" ")[0].toLong()
        val q100 = l.cpuQuotaForTest(100).split(" ")[0].toLong()

        assertTrue(q10 < q50)
        assertTrue(q50 < q100)
    }

    @Test
    fun `cpuCount falls back on invalid cpu max`() {
        val fs = FakeFs()
        fs.mkdirs("/sys/fs/cgroup")
        fs.writeText("/sys/fs/cgroup/cpu.max", "invalid data")

        val l = limiter(fs)

        assertEquals(Runtime.getRuntime().availableProcessors(), l.cpuCount())
    }

}

// Helpers
private fun LinuxCpuLimiterService.cpuQuotaForTest(percent: Int) =
    this::class.java.getDeclaredMethod("cpuQuota", Int::class.java)
        .apply { isAccessible = true }
        .invoke(this, percent) as String
