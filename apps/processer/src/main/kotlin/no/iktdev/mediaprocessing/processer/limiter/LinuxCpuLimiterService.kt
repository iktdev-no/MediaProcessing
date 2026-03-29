package no.iktdev.mediaprocessing.processer.limiter

import java.io.File

class LinuxCpuLimiterService : CpuLimiterService {

    private val base = File("/sys/fs/cgroup")

    private fun processExists(pid: Long): Boolean =
        ProcessHandle.of(pid).map { it.isAlive }.orElse(false) ?: false

    override fun limitProcess(pid: Long, percent: Int) {
        if (percent >= 100) {
            // 100% = ingen limit → sørg for at cgroup ikke eksisterer
            removeLimit(pid)
            return
        }

        if (!processExists(pid)) return

        val group = File(base, "ffmpeg-$pid")
        group.mkdirs()

        val quota = (percent * 1000).coerceAtLeast(1000)

        File(group, "cpu.max").writeText("$quota 100000")
        File(group, "cgroup.procs").writeText(pid.toString())
    }

    override fun updateLimit(pid: Long, percent: Int) {
        if (percent >= 100) {
            removeLimit(pid)
            return
        }

        if (!processExists(pid)) {
            removeLimit(pid)
            return
        }

        val group = File(base, "ffmpeg-$pid")
        val quota = (percent * 1000).coerceAtLeast(1000)
        File(group, "cpu.max").writeText("$quota 100000")
    }

    override fun removeLimit(pid: Long) {
        val group = File(base, "ffmpeg-$pid")

        // Hvis prosessen fortsatt lever → ikke slett cgroup
        if (processExists(pid)) return

        group.deleteRecursively()
    }
}
