package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LimiterDeterminismTest {

    @Test
    fun `same sequence produces identical filesystem state`() {
        fun runSequence(): String {
            val fs = FakeFs()
            val l = LinuxCpuLimiterService(fs)

            fs.mkdirs("/proc/1")
            fs.mkdirs("/proc/2")
            fs.writeText("/proc/1/cgroup", "0::/user.slice")
            fs.writeText("/proc/2/cgroup", "0::/user.slice")

            l.limitProcess(1, 25)
            l.limitProcess(2, 25)
            l.updateLimit(1, 50)
            l.removeLimit(2)

            return fs.dump()
        }

        val a = runSequence()
        val b = runSequence()

        assertEquals(a, b)
    }
}
