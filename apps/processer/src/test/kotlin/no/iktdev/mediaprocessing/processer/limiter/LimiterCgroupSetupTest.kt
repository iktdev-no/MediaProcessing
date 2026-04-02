package no.iktdev.mediaprocessing.processer.limiter


import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LimiterCgroupSetupTest {

    @Test
    fun `ensureRoot enables cpu and cpuset controllers without breaking existing`() {
        val fs = FakeFs()

        fs.mkdirs("/sys/fs/cgroup")
        fs.writeText("/sys/fs/cgroup/cgroup.subtree_control", "memory io")

        LinuxCpuLimiterService(fs)

        val content = fs.readText("/sys/fs/cgroup/cgroup.subtree_control") ?: ""

        assertTrue(content.contains("cpu"))
        assertTrue(content.contains("cpuset"))
    }

    @Test
    fun `ensureRoot does nothing if controllers already enabled`() {
        val fs = FakeFs()

        fs.mkdirs("/sys/fs/cgroup")
        fs.writeText("/sys/fs/cgroup/cgroup.subtree_control", "cpu cpuset")

        val before = fs.readText("/sys/fs/cgroup/cgroup.subtree_control")

        LinuxCpuLimiterService(fs)

        val after = fs.readText("/sys/fs/cgroup/cgroup.subtree_control")

        assertEquals(before, after)
    }
}