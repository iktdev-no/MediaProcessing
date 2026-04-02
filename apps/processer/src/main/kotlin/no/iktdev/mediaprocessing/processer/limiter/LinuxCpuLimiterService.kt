package no.iktdev.mediaprocessing.processer.limiter

import no.iktdev.mediaprocessing.processer.services.fs.IFs
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

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

    internal open fun supportsLimit(): Boolean =
        supportDetails().values.all { it }


    internal open fun isCgroupV2Mounted(): Boolean {
        val mounts = fs.readLines("/proc/mounts") ?: return false
        return mounts.any { it.contains("cgroup2") }
    }

    internal open fun supportDetails(): Map<String, Boolean> {
        val details = mutableMapOf<String, Boolean>()

        val controllers = "$rootPath/cgroup.controllers"
        val hasCgroupV2 = fs.exists(controllers)
        details["cgroup_v2"] = hasCgroupV2

        if (!hasCgroupV2) return details

        val content = fs.readText(controllers) ?: ""
        val controllersList = content.split(Regex("\\s+"))
        details["cpu_controller"] = "cpu" in controllersList
        details["cpuset_controller"] = "cpuset" in controllersList
        details["cgroup2_mounted"] = isCgroupV2Mounted()

        val subtree = "$rootPath/cgroup.subtree_control"
        val subtreeExists = fs.exists(subtree)
        details["subtree_exists"] = subtreeExists

        return details
    }


    internal fun supportReport(): String {
        val d = supportDetails()
        return buildString {
            appendLine("CPU limiting support:")
            d.forEach { (k, v) ->
                appendLine(" - $k: ${if (v) "OK" else "MISSING"}")
            }
        }
    }



    // ---------------------------
    // ROOT SETUP (kernel-safe)
    // ---------------------------

    private fun ensureRoot() {
        if (!supportsLimit()) return
        if (!fs.exists(appRootPath)) {
            fs.mkdirs(appRootPath)
        }

        val control = "$rootPath/cgroup.subtree_control"
        if (!fs.exists(control)) return

        try {
            val current1 = fs.readText(control) ?: ""
            if (!current1.contains("cpu")) {
                fs.writeText(control, "+cpu")
            }

            val current2 = fs.readText(control) ?: ""
            if (!current2.contains("cpuset")) {
                fs.writeText(control, "+cpuset")
            }
        } catch (_: Exception) {}
    }

    // ---------------------------
    // PROCESS
    // ---------------------------

    internal open fun alive(pid: Long): Boolean =
        ProcessHandle.of(pid).map { it.isAlive }.orElse(false) ?: false


    private fun groupPath(pid: Long): String =
        "$appRootPath/ffmpeg-$pid"

    private fun movePid(path: String, pid: Long) {
        repeat(3) {
            if (fs.writeText("$path/cgroup.procs", pid.toString())) return
            Thread.sleep(1)
        }
        println("Failed to move pid=$pid to $path")
    }

    // ---------------------------
    // CPU QUOTA
    // ---------------------------

    @VisibleForTesting
    internal fun cpuCount(): Int {
        val cpuMaxPath = "$rootPath/cpu.max"

        if (fs.exists(cpuMaxPath)) {
            val parts = fs.readText(cpuMaxPath)?.trim()?.split(" ") ?: emptyList()
            if (parts.size == 2) {
                val quotaStr = parts[0]
                val period = parts[1].toLongOrNull()

                if (quotaStr != "max" && period != null && period > 0) {
                    val quota = quotaStr.toLongOrNull()
                    if (quota != null && quota > 0) {
                        return ceil(quota.toDouble() / period).toInt().coerceAtLeast(1)
                    }
                }
            }
        }

        return Runtime.getRuntime().availableProcessors()
    }

    private fun cpuQuota(percent: Int): String {
        val total = cpuCount()
        val allowed = total * (percent / 100.0)
        val quota = (allowed * 100_000L).toLong().coerceAtLeast(1_000L)
        return "$quota 100000"
    }

    // ---------------------------
    // CPUSET
    // ---------------------------

    internal fun assignCpuset(pid: Long, percent: Int): List<Int> {
        val total = cpuCount()
        val cores = (total * (percent / 100.0))
            .toInt()
            .coerceAtLeast(1)
            .coerceAtMost(total)

        synchronized(coreLock) {
            val list = mutableListOf<Int>()
            repeat(cores) {
                list.add(nextCore)
                nextCore = (nextCore + 1) % total
            }
            assignedCores[pid] = list
            assignedPercent[pid] = percent
            return list
        }
    }

    private fun getOrAssignCores(pid: Long, percent: Int): List<Int> {
        val existing = assignedCores[pid]
        val prevPercent = assignedPercent[pid]
        return if (existing != null && prevPercent == percent) existing
        else assignCpuset(pid, percent)
    }

    private fun initCpuset(gPath: String) {
        val mems =
            fs.readText("$appRootPath/cpuset.mems")?.trim()?.ifBlank { null }
                ?: fs.readText("$rootPath/cpuset.mems")?.trim()
                ?: "0"

        fs.writeText("$gPath/cpuset.mems", mems)
    }

    // ---------------------------
    // LIMIT
    // ---------------------------

    override fun limitProcess(pid: Long, percent: Int) {
        ensureRoot()

        if (!alive(pid)) return
        if (percent >= 100) return removeLimit(pid)

        val gPath = groupPath(pid)
        if (!fs.exists(gPath)) fs.mkdirs(gPath)

        originalCgroups.putIfAbsent(pid, detectCgroupPath(pid))

        try {
            initCpuset(gPath)

            if (!fs.writeText("$gPath/cpu.max", cpuQuota(percent))) {
                println("limit: failed cpu.max pid=$pid")
            }

            movePid(gPath, pid)

            val cores = getOrAssignCores(pid, percent)
            if (!fs.writeText("$gPath/cpuset.cpus", cores.joinToString(","))) {
                println("limit: failed cpuset pid=$pid")
            }

        } catch (e: Exception) {
            println("limit failed pid=$pid: ${e.message}")
        }
    }

    // ---------------------------
    // UPDATE
    // ---------------------------

    override fun updateLimit(pid: Long, percent: Int) {
        ensureRoot()

        if (!alive(pid)) {
            removeLimit(pid)
            return
        }

        if (percent >= 100) {
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

            if (!fs.writeText("$gPath/cpu.max", cpuQuota(percent))) {
                println("update: failed cpu.max pid=$pid")
            }

            movePid(gPath, pid)

            val cores = getOrAssignCores(pid, percent)
            if (!fs.writeText("$gPath/cpuset.cpus", cores.joinToString(","))) {
                println("update: failed cpuset pid=$pid")
            }

        } catch (e: Exception) {
            println("update failed pid=$pid: ${e.message}")
        }
    }

    // ---------------------------
    // REMOVE
    // ---------------------------

    override fun removeLimit(pid: Long) {
        ensureRoot()

        val gPath = groupPath(pid)
        val original = originalCgroups.remove(pid)

        assignedCores.remove(pid)
        assignedPercent.remove(pid)

        if (assignedCores.isEmpty()) {
            synchronized(coreLock) {
                nextCore = 0
            }
        }

        if (!fs.exists(gPath)) return

        try {
            if (!original.isNullOrBlank() && alive(pid)) {
                val targetPath = "$rootPath/${original.removePrefix("/")}"
                if (fs.exists(targetPath)) {
                    movePid(targetPath, pid)
                }
            }

            if (!fs.deleteRecursively(gPath)) {
                println("remove: failed to delete $gPath (maybe busy)")
            }

        } catch (e: Exception) {
            println("remove failed pid=$pid: ${e.message}")
        }
    }

    // ---------------------------
    // CGROUP DETECTION
    // ---------------------------

    private fun detectCgroupPath(pid: Long): String {
        val path = "/proc/$pid/cgroup"
        val lines = fs.readLines(path) ?: return ""
        return lines.firstOrNull { it.startsWith("0::") }
            ?.substringAfter("0::")
            ?.trim()
            ?: ""
    }
}