package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LimiterEdgeCaseTest {

    @Test
    fun `limitProcess does nothing when pid is dead`() {
        val fs = FakeFs()
        val l = LinuxCpuLimiterService(fs)

        l.limitProcess(999999, 50)

        assertTrue(fs.writeLog.isEmpty())
    }

    @Test
    fun `removeLimit does not crash if original cgroup is missing`() {
        val fs = FakeFs()

        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        val l = LinuxCpuLimiterService(fs)

        l.limitProcess(1, 50)

        // Simuler at original cgroup er borte
        fs.deleteRecursively("/sys/fs/cgroup/user.slice")

        assertDoesNotThrow {
            l.removeLimit(1)
        }
    }

    @Test
    fun `updateLimit falls back to limitProcess when cgroup missing`() {
        val fs = FakeFs()
        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        val l = LinuxCpuLimiterService(fs)

        l.updateLimit(1, 50)

        assertTrue(fs.exists("/sys/fs/cgroup/mediaprocessing/ffmpeg-1"))
    }

    @Test
    fun `initCpuset falls back to root cpuset mems`() {
        val fs = FakeFs()
        fs.mkdirs("/sys/fs/cgroup")
        fs.writeText("/sys/fs/cgroup/cpuset.mems", "0")

        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        val l = LinuxCpuLimiterService(fs)
        l.limitProcess(1, 50)

        assertEquals("0", fs.readText("/sys/fs/cgroup/mediaprocessing/ffmpeg-1/cpuset.mems"))
    }
}
