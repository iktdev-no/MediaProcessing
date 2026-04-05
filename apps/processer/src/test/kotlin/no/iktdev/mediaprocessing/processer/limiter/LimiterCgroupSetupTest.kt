package no.iktdev.mediaprocessing.processer.limiter


import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LimiterCgroupSetupTest {

    private class TestLimiter(fs: FakeFs) : LinuxCpuLimiterService(fs) {
        override fun supportsLimit(): Boolean = true
    }


    @Test
    fun `ensureRoot enables cpu and cpuset controllers without breaking existing`() {
        val fs = FakeFs()

        fs.mkdirs("/cgroup")

        // Required for supportsLimit() to return true
        fs.writeText("/cgroup/cgroup.controllers", "cpu cpuset memory io")

        fs.writeText("/cgroup/cgroup.subtree_control", "memory io")

        TestLimiter(fs)

        val content = fs.readText("/cgroup/cgroup.subtree_control") ?: ""

        assertTrue(content.contains("+cpu"))
        assertTrue(content.contains("+cpuset"))
        assertTrue(content.contains("memory"))
        assertTrue(content.contains("io"))
    }


    @Test
    fun `ensureRoot does nothing if controllers already enabled`() {
        val fs = FakeFs()

        fs.mkdirs("/cgroup")
        fs.writeText("/cgroup/cgroup.subtree_control", "cpu cpuset")

        val before = fs.readText("/cgroup/cgroup.subtree_control")

        LinuxCpuLimiterService(fs)

        val after = fs.readText("/cgroup/cgroup.subtree_control")

        assertEquals(before, after)
    }
}