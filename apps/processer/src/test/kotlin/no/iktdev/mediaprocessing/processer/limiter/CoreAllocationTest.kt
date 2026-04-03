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

    @Test
    fun `global pinning overrides implicit pinning`() {
        val l = limiter()

        l.setGlobalPinnedCores(listOf(2, 3))

        val c1 = l.limitAndGetEffective(1, 25)
        val c2 = l.limitAndGetEffective(2, 50)
        val c3 = l.limitAndGetEffective(3, 10)

        assertEquals(listOf(2, 3), c1)
        assertEquals(listOf(2, 3), c2)
        assertEquals(listOf(2, 3), c3)
    }

    @Test
    fun `global pinning overrides per pid pinning`() {
        val l = limiter()

        l.pinProcessToCores(10, listOf(7))
        l.setGlobalPinnedCores(listOf(1, 2))

        val c = l.limitAndGetEffective(10, 25)

        assertEquals(listOf(1, 2), c)
    }

    @Test
    fun `global pinning applies to all processes`() {
        val l = limiter()

        l.setGlobalPinnedCores(listOf(4, 5))

        val results = (1..5).map { pid ->
            l.limitAndGetEffective(pid.toLong(), 30)
        }

        results.forEach { cores ->
            assertEquals(listOf(4, 5), cores)
        }
    }

    @Test
    fun `removing global pinning restores implicit pinning`() {
        val l = limiter()

        val total = l.cpuCount()
        val coresPerProcess = max(1, (total * 25) / 100)

        l.setGlobalPinnedCores(listOf(8, 9))

        // Under global pinning
        val g1 = l.limitAndGetEffective(1, 25)
        assertEquals(listOf(8, 9), g1)

        // Remove global pinning
        l.setGlobalPinnedCores(null)

        // Now implicit pinning should resume
        val c1 = l.limitAndGetEffective(2, 25)
        val c2 = l.limitAndGetEffective(3, 25)

        assertEquals((0 until coresPerProcess).toList(), c1)
        assertEquals((coresPerProcess until coresPerProcess * 2).toList(), c2)
    }

    @Test
    fun `per pid pinning overrides implicit pinning`() {
        val l = limiter()

        l.pinProcessToCores(10, listOf(4, 5))

        val cores = l.limitAndGetEffective(10, 25)

        assertEquals(listOf(4, 5), cores)
    }

    @Test
    fun `per pid pinning is stable`() {
        val l = limiter()

        l.pinProcessToCores(20, listOf(7))

        val c1 = l.limitAndGetEffective(20, 10)
        val c2 = l.limitAndGetEffective(20, 50)

        assertEquals(c1, c2)
        assertEquals(listOf(7), c1)
    }

    @Test
    fun `per pid pinning does not affect other processes`() {
        val l = limiter()

        l.pinProcessToCores(1, listOf(3))

        val c1 = l.limitAndGetEffective(1, 25)
        val c2 = l.limitAndGetEffective(2, 25)

        assertEquals(listOf(3), c1)
        assertNotEquals(c1, c2) // implicit pinning for pid 2
    }

    @Test
    fun `removeLimit clears per pid pinning`() {
        val l = limiter()

        l.pinProcessToCores(5, listOf(6))

        val pinned = l.limitAndGetEffective(5, 25)
        assertEquals(listOf(6), pinned)

        l.removeLimit(5)

        val total = l.cpuCount()
        val coresPerProcess = max(1, (total * 25) / 100)

        val implicit = l.limitAndGetEffective(5, 25)

        assertEquals((0 until coresPerProcess).toList(), implicit)
    }


    private fun LinuxCpuLimiterService.limitProcessAndGetCores(pid: Long, percent: Int): List<Int> {
        this.limitProcess(pid, percent)
        return this.assignedCores[pid] ?: emptyList()
    }

    private fun LinuxCpuLimiterService.limitAndGetEffective(pid: Long, percent: Int): List<Int> {
        this.limitProcess(pid, percent)
        return this.getEffectiveCores(pid) ?: emptyList()
    }


}
