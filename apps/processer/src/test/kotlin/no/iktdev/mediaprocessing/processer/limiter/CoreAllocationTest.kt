package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.FakeFs
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.max

internal class CoreAllocationTest {

    internal class TestLimiter(fs: FakeFs) : LinuxCpuLimiterService(fs) {
        override fun alive(pid: Long) = true
    }

    private fun limiter(): LinuxCpuLimiterService = TestLimiter(FakeFs())

    @Test
    fun `round robin allocates sequential cores`() {
        val l = limiter()

        val total = l.cpuCount()
        val coresPerProcess = max(1, (total * 25) / 100)

        val c1 = l.limitProcessAndGetCores(1, 25)
        val c2 = l.limitProcessAndGetCores(2, 25)
        val c3 = l.limitProcessAndGetCores(3, 25)

        assertEquals((0 until coresPerProcess).toList(), c1)
        assertEquals((coresPerProcess until coresPerProcess * 2).toList(), c2)
        assertEquals((coresPerProcess * 2 until coresPerProcess * 3).toList(), c3)
    }


    @Test
    fun `wrap around when exceeding total cores`() {
        val l = limiter()

        val total = l.cpuCount()
        val coresPerProcess = max(1, (total * 25) / 100)

        val results = (0 until total + 2).map { pid ->
            l.limitProcessAndGetCores(pid.toLong(), 25)
        }

        assertEquals((0 until coresPerProcess).toList(), results[0])
        assertEquals((coresPerProcess until coresPerProcess * 2).toList(), results[1])
        assertEquals((0 until coresPerProcess).toList(), results[total]) // wrap
    }


    @Test
    fun `same percent returns same core assignment`() {
        val l = limiter()

        val c1 = l.limitProcessAndGetCores(10, 50)
        val c2 = l.limitProcessAndGetCores(10, 50)

        assertEquals(c1, c2)
    }

    @Test
    fun `changing percent triggers new core assignment`() {
        val l = limiter()

        val c1 = l.limitProcessAndGetCores(10, 25)
        val c2 = l.limitProcessAndGetCores(10, 50)

        assertNotEquals(c1, c2)
    }

    @Test
    fun `same sequence of allocations produces identical core mapping`() {
        fun run(): List<List<Int>> {
            val l = limiter()
            return listOf(
                l.limitProcessAndGetCores(1, 25),
                l.limitProcessAndGetCores(2, 25),
                l.limitProcessAndGetCores(3, 50)
            )
        }

        assertEquals(run(), run())
    }


    private fun LinuxCpuLimiterService.limitProcessAndGetCores(pid: Long, percent: Int): List<Int> {
        this.limitProcess(pid, percent)
        return this.assignedCores[pid] ?: emptyList()
    }

}
