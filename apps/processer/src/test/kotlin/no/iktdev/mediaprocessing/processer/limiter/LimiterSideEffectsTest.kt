package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LimiterSideEffectsTest {

    internal class TestLimiter(fs: FakeFs) : LinuxCpuLimiterService(fs) {
        override fun alive(pid: Long) = true
    }

    @Test
    fun `limitProcess writes files in correct order`() {
        val fs = FakeFs()
        fs.mkdirs("/proc/123")
        fs.writeText("/proc/123/cgroup", "0::/user.slice")

        // Governor expects this directory to exist
        fs.mkdirs("/sys/fs/cgroup/user.slice")

        val l = TestLimiter(fs)
        l.limitProcess(123, 50)

        val writes = fs.writeLog
            .filter { it.path.startsWith("/sys/fs/cgroup") }

        assertEquals("cpuset.mems", writes[0].file)
        assertEquals("cpu.max", writes[1].file)
        assertEquals("cgroup.procs", writes[2].file)
        assertEquals("cpuset.cpus", writes[3].file)
    }

    @Test
    fun `removeLimit cleans up internal state`() {
        val fs = FakeFs()
        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        val l = TestLimiter(fs)

        l.limitProcess(1, 50)
        l.removeLimit(1)

        assertTrue(l.assignedCores[1].isNullOrEmpty())
    }

    @Test
    fun `updateLimit does not reassign cores when percent unchanged`() {
        val fs = FakeFs()
        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        val l = TestLimiter(fs)

        l.limitProcess(1, 50)
        val c1 = l.assignedCores[1]

        l.updateLimit(1, 50)
        val c2 = l.assignedCores[1]

        assertEquals(c1, c2)
    }

    @Test
    fun `updateLimit reassigns cores when percent changes`() {
        val fs = FakeFs()
        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        val l = TestLimiter(fs)

        l.limitProcess(1, 25)
        val c1 = l.assignedCores[1]

        l.updateLimit(1, 50)
        val c2 = l.assignedCores[1]

        assertNotEquals(c1, c2)
    }

    @Test
    fun `removeLimit restores original cgroup and deletes directory`() {
        val fs = FakeFs()
        fs.mkdirs("/proc/1")
        fs.writeText("/proc/1/cgroup", "0::/user.slice")

        // Governor expects this directory to exist
        fs.mkdirs("/sys/fs/cgroup/user.slice")

        val l = TestLimiter(fs)

        l.limitProcess(1, 50)
        l.removeLimit(1)

        assertTrue(fs.exists("/sys/fs/cgroup/user.slice/cgroup.procs"))
        assertFalse(fs.exists("/sys/fs/cgroup/mediaprocessing/ffmpeg-1"))
    }
}