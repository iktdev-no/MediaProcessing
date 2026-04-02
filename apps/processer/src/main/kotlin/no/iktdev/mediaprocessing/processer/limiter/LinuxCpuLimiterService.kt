package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.IFs
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.ConcurrentHashMap

internal open class LinuxCpuLimiterService(
    private val fs: IFs
) : CpuLimiterService {

    private val rootPath = "/sys/fs/cgroup"
    private val appRootPath = "$rootPath/mediaprocessing"

    private val originalCgroups = ConcurrentHashMap<Long, String>()
    internal val assignedCores = ConcurrentHashMap<Long, List<Int>>()
    private val assignedPercent = ConcurrentHashMap<Long, Int>()

    private var nextCore = 0
    private val coreLock = Any()

    init {
        ensureRoot()
    }

    // ---------------------------------------------------------
    // SUPPORT CHECK
    // ---------------------------------------------------------

    internal open fun supportsLimit(): Boolean =
        supportDetails().values.all { it }

    internal open fun isCgroupV2Mounted(): Boolean {
        val mounts = fs.readLines("/proc/mounts") ?: return false
        return mounts.any { it.contains("cgroup2") }
    }

    internal open fun supportDetails(): Map<String, Boolean> {
        val controllers = "$rootPath/cgroup.controllers"
        val has = fs.exists(controllers)

        if (!has) {
            return mapOf(
                "cgroup_v2" to false,
                "cpu_controller" to false,
                "cpuset_controller" to false,
                "cgroup2_mounted" to false,
                "subtree_exists" to false
            )
        }

        val list = fs.readText(controllers)
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        return mapOf(
            "cgroup_v2" to true,
            "cpu_controller" to ("cpu" in list),
            "cpuset_controller" to ("cpuset" in list),
            "cgroup2_mounted" to isCgroupV2Mounted(),
            "subtree_exists" to fs.exists("$rootPath/cgroup.subtree_control")
        )
    }

    internal fun supportReport(): String =
        buildString {
            appendLine("CPU limiting support:")
            supportDetails().forEach { (k, v) ->
                appendLine(" - $k: ${if (v) "OK" else "MISSING"}")
            }
        }

    // ---------------------------------------------------------
    // ROOT SETUP
    // ---------------------------------------------------------

    private fun ensureRoot() {
        if (!supportsLimit()) return

        if (!fs.exists(appRootPath)) {
            fs.mkdirs(appRootPath)
        }

        val control = "$rootPath/cgroup.subtree_control"
        if (!fs.exists(control)) return

        try {
            val currentSet = fs.readText(control)
                ?.trim()
                ?.split(" ")
                ?.filter { it.isNotBlank() }
                ?.toMutableSet()
                ?: mutableSetOf()

            val changed = currentSet.add("+cpu") or currentSet.add("+cpuset")

            if (changed) {
                fs.writeText(control, currentSet.joinToString(" "))
            }

        } catch (e: Exception) {
            println("ensureRoot failed: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // PROCESS
    // ---------------------------------------------------------

    internal open fun alive(pid: Long): Boolean =
        ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

    private fun groupPath(pid: Long) =
        "$appRootPath/ffmpeg-$pid"

    private fun movePid(path: String, pid: Long) {
        var delay = 5L
        repeat(5) {
            if (fs.writeText("$path/cgroup.procs", pid.toString())) return
            Thread.sleep(delay)
            delay *= 2
        }
        println("Failed to move pid=$pid to $path")
    }

    // ---------------------------------------------------------
    // CPU COUNT
    // ---------------------------------------------------------

    @VisibleForTesting
    internal fun cpuCount(): Int {
        val raw = fs.readText("$rootPath/cpuset.cpus.effective")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return Runtime.getRuntime().availableProcessors()

        val cores = raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .flatMap { part ->
                if ("-" in part) {
                    val (s, e) = part.split("-", limit = 2)
                    val start = s.toIntOrNull()
                    val end = e.toIntOrNull()
                    if (start != null && end != null && end >= start)
                        (start..end).toList()
                    else emptyList()
                } else {
                    part.toIntOrNull()?.let { listOf(it) } ?: emptyList()
                }
            }
            .toSet()

        return cores.size.takeIf { it > 0 }
            ?: Runtime.getRuntime().availableProcessors()
    }

    // ---------------------------------------------------------
    // CPU QUOTA
    // ---------------------------------------------------------

    internal open fun cpuQuota(gPath: String, percent: Int): String {
        val cpuMaxRaw =
            fs.readText("$gPath/cpu.max")
                ?: fs.readText("$rootPath/cpu.max")
                ?: "100000 100000"

        val parts = cpuMaxRaw.trim().split(" ")

        if (parts.firstOrNull() == "max") {
            return "max"
        }

        val period = parts.getOrNull(1)?.toLongOrNull() ?: 100_000L
        val quota = (period * percent / 100).coerceAtLeast(1_000L)

        return "$quota $period"
    }

    // ---------------------------------------------------------
    // CPUSET
    // ---------------------------------------------------------

    internal fun assignCpuset(pid: Long, percent: Int): List<Int> {
        val total = cpuCount()
        val cores = (total * percent / 100.0)
            .toInt()
            .coerceIn(1, total)

        synchronized(coreLock) {
            val list = List(cores) {
                val c = nextCore
                nextCore = (nextCore + 1) % total
                c
            }

            assignedCores[pid] = list
            assignedPercent[pid] = percent

            return list
        }
    }

    private fun getOrAssignCores(pid: Long, percent: Int): List<Int> {
        val existing = assignedCores[pid]
        val prev = assignedPercent[pid]
        return if (existing != null && prev == percent) existing
        else assignCpuset(pid, percent)
    }

    private fun initCpuset(gPath: String) {
        val mems =
            fs.readText("$appRootPath/cpuset.mems")?.trim()?.ifBlank { null }
                ?: fs.readText("$rootPath/cpuset.mems")?.trim()
                ?: "0"

        fs.writeText("$gPath/cpuset.mems", mems)
    }

    // ---------------------------------------------------------
    // LIMIT
    // ---------------------------------------------------------

    override fun limitProcess(pid: Long, percent: Int) {
        ensureRoot()

        if (!alive(pid)) return
        if (percent >= 100) return removeLimit(pid)

        val gPath = groupPath(pid)
        if (!fs.exists(gPath)) fs.mkdirs(gPath)

        originalCgroups.putIfAbsent(pid, detectCgroupPath(pid))

        try {
            initCpuset(gPath)

            fs.writeText("$gPath/cpu.max", cpuQuota(gPath, percent))
            movePid(gPath, pid)

            val cores = getOrAssignCores(pid, percent)
            fs.writeText("$gPath/cpuset.cpus", cores.joinToString(","))

        } catch (e: Exception) {
            println("limit failed pid=$pid: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    override fun updateLimit(pid: Long, percent: Int) {
        ensureRoot()

        if (!alive(pid)) {
            removeLimit(pid)
            return
        }

        val gPath = groupPath(pid)
        if (!fs.exists(gPath)) {
            limitProcess(pid, percent)
            return
        }

        try {
            initCpuset(gPath)

            fs.writeText("$gPath/cpu.max", cpuQuota(gPath, percent))
            movePid(gPath, pid)

            val cores = getOrAssignCores(pid, percent)
            fs.writeText("$gPath/cpuset.cpus", cores.joinToString(","))

        } catch (e: Exception) {
            println("update failed pid=$pid: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // REMOVE
    // ---------------------------------------------------------

    override fun removeLimit(pid: Long) {
        val gPath = groupPath(pid)
        val original = originalCgroups.remove(pid)

        assignedCores.remove(pid)
        assignedPercent.remove(pid)

        if (assignedCores.isEmpty() && assignedPercent.isEmpty()) {
            synchronized(coreLock) {
                if (assignedCores.isEmpty()) {
                    nextCore = 0
                }
            }
        }

        if (!fs.exists(gPath)) return

        try {
            if (!original.isNullOrBlank() && alive(pid)) {
                val target = "$rootPath/${original.removePrefix("/")}"
                if (fs.exists(target)) movePid(target, pid)
            }

            runCatching {
                fs.deleteRecursively(gPath)
            }

        } catch (e: Exception) {
            println("remove failed pid=$pid: ${e.message}")
        }
    }

    // ---------------------------------------------------------
    // CGROUP DETECTION
    // ---------------------------------------------------------

    private fun detectCgroupPath(pid: Long): String {
        val lines = fs.readLines("/proc/$pid/cgroup") ?: return ""
        return lines.firstOrNull { it.contains("::/") }
            ?.substringAfter("::")
            ?.trim()
            ?: ""
    }
}