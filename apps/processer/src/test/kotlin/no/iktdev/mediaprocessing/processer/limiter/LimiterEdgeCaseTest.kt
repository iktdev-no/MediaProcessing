package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LimiterEdgeCaseTest {

    private class TestLimiter(fs: FakeFs) : LinuxCpuLimiterService(fs) {
         override fun supportsLimit(): Boolean = true
    }

    fun FakeFs.mockCgroupV2Environment() {
        mkdirs("/sys/fs/cgroup")
        writeText("/sys/fs/cgroup/cgroup.procs", "1")
        writeText("/sys/fs/cgroup/cgroup.controllers", "cpu cpuset")
        writeText("/sys/fs/cgroup/cgroup.subtree_control", "+cpu +cpuset")
        writeText("/sys/fs/cgroup/cpuset.mems", "0")

        mkdirs("/proc")
        writeText("/proc/mounts", "cgroup2 /sys/fs/cgroup cgroup2 rw 0 0")
    }


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
        val fs = FakeFs().also { it.mockCgroupV2Environment() }
        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        val l = TestLimiter(fs)

        l.updateLimit(1, 50)

        assertTrue(fs.exists("/sys/fs/cgroup/processer/ffmpeg-1"))
    }

    @Test
    fun `initCpuset falls back to root cpuset mems`() {
        val fs = FakeFs().also { it.mockCgroupV2Environment() }
        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        val l = TestLimiter(fs)
        l.limitProcess(1, 50)

        assertEquals("0", fs.readText("/sys/fs/cgroup/processer/ffmpeg-1/cpuset.mems"))
    }
}
