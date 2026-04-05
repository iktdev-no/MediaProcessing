package no.iktdev.mediaprocessing.processer.limiter

import mu.KotlinLogging
import no.iktdev.mediaprocessing.processer.services.fs.IFs
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.CpuLimitSupport
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.LinuxCpuLimitSupport
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.ConcurrentHashMap

internal open class LinuxCpuLimiterService(
    private val fs: IFs
) : CpuLimiterService {

    private val log = KotlinLogging.logger {}

    private fun getRootPath(): String = "/cgroup"

    private val appRootPath = getRootPath()
    private val appPrefix = "processer"

    private val originalCgroups = ConcurrentHashMap<Long, String>()
    internal val assignedCores = ConcurrentHashMap<Long, List<Int>>()
    private val assignedPercent = ConcurrentHashMap<Long, Int>()
    private var globalPinnedCores: List<Int>? = null
    private val manuallyPinned = ConcurrentHashMap<Long, List<Int>>()


    private var nextCore = 0
    private val coreLock = Any()

    init {
        ensureRoot()
        logSupport()
    }

    fun logSupport() {
        val s = detectSupportsCpuLimits()

        val block = buildString {
            appendLine("\n──────────────────────────────────────────────")
            appendLine(" CPU LIMIT SUPPORT (${s.os})")
            appendLine("──────────────────────────────────────────────")
            appendLine(" supported: ${s.supported}")
            appendLine(" reason: ${s.reason ?: "OK"}")

            if (s is LinuxCpuLimitSupport) {
                appendLine(" cgroup v2:           ${s.cgroupV2}")
                appendLine(" cpu controller:      ${s.cpuController}")
                appendLine(" cpuset controller:   ${s.cpusetController}")
                appendLine(" cgroup mounted:      ${s.cgroupMounted}")
                appendLine(" subtree control:     ${s.subtreeControlExists}")
            }

            appendLine("──────────────────────────────────────────────")
        }

        log.info { block }
    }


    override fun setGlobalPinnedCores(cores: List<Int>?) {
        globalPinnedCores = cores
    }

    override fun pinProcessToCores(pid: Long, cores: List<Int>) {
        manuallyPinned[pid] = cores
        assignedCores[pid] = cores
        assignedPercent[pid] = -1
    }

    override fun getGlobalPinnedCores(): List<Int>? = globalPinnedCores
    override fun isGlobalPinningActive(): Boolean = globalPinnedCores != null

    override fun getManuallyPinnedCores(pid: Long): List<Int>? = manuallyPinned[pid]

    override fun getAssignedCores(pid: Long): List<Int>? = assignedCores[pid]
    override fun getCpuCount(): Int = cpuCount()
    override fun getPercentLimit(pid: Long): Int? = assignedPercent[pid]

    override fun getEffectiveCores(pid: Long): List<Int>? =
        globalPinnedCores
            ?: manuallyPinned[pid]
            ?: assignedCores[pid]

    fun getCpuLimitSupport(): CpuLimitSupport = detectSupportsCpuLimits()


    // ---------------------------------------------------------
    // SUPPORT CHECK
    // ---------------------------------------------------------



    internal open fun supportsLimit(): Boolean =
        detectSupportsCpuLimits().supported


    internal open fun isCgroupV2Mounted(): Boolean {
        val mounts = fs.readLines("/proc/mounts") ?: return false
        return mounts.any { it.contains("cgroup2") }
    }

    override fun detectSupportsCpuLimits(): CpuLimitSupport {
        val controllersPath = "${getRootPath()}/cgroup.controllers"

        if (!fs.exists(controllersPath)) {
            return LinuxCpuLimitSupport(
                supported = false,
                reason = "cgroup.controllers not found (not cgroup v2?)"
            )
        }

        val controllers = fs.readText(controllersPath)
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val cpu = "cpu" in controllers
        val cpuset = "cpuset" in controllers
        val subtree = fs.exists("${getRootPath()}/cgroup.subtree_control")
        val mounted = isCgroupV2Mounted()

        val supported = cpu && cpuset && subtree && mounted

        return LinuxCpuLimitSupport(
            supported = supported,
            reason = if (supported) null else "Missing required controllers or subtree",
            cgroupV2 = true,
            cpuController = cpu,
            cpusetController = cpuset,
            cgroupMounted = mounted,
            subtreeControlExists = subtree
        )
    }


    // ---------------------------------------------------------
    // ROOT SETUP
    // ---------------------------------------------------------

    private fun ensureRoot(): Boolean {
        val root = getRootPath()

        // 1. Root must exist (bind mount)
        if (!fs.exists(root)) {
            log.error { "cgroup root $root does not exist. Missing bind mount?" }
            return false
        }

        // 2. Root must be writable
        if (!fs.canWrite(root)) {
            log.error { "cgroup root $root is not writable. Bind mount must be RW." }
            return false
        }

        // 3. Must be cgroup v2 (controllers file must exist)
        val controllersPath = "$root/cgroup.controllers"
        if (!fs.exists(controllersPath)) {
            log.error { "cgroup.controllers missing in $root. Not cgroup v2 or wrong mount." }
            return false
        }

        // 4. subtree_control must exist
        val subtree = "$root/cgroup.subtree_control"
        if (!fs.exists(subtree)) {
            log.error { "cgroup.subtree_control missing in $root. Cannot enable controllers." }
            return false
        }

        // 5. Enable +cpu and +cpuset
        try {
            val currentSet = fs.readText(subtree)
                ?.trim()
                ?.split(" ")
                ?.filter { it.isNotBlank() }
                ?.toMutableSet()
                ?: mutableSetOf()
            log.info("Found subtree_control controllers: ${currentSet.joinToString(", ")}")

            var requiresUpdate = false
            if (!currentSet.contains("cpu")) {
                currentSet.add("+cpu")
                requiresUpdate = true
            }

            if (!currentSet.contains("cpuset")) {
                currentSet.add("+cpuset")
                requiresUpdate = true
            }

            if (requiresUpdate) {
                if (!fs.writeText(subtree, currentSet.joinToString(" "))) {
                    log.error { "Failed to write subtree_control in $root" }
                    return false
                } else {
                    log.info { "Updated subtree_control to: ${currentSet.joinToString(", ")}" }
                }
            }

        } catch (e: Exception) {
            log.error { "Failed to initialize subtree_control: ${e.message}" }
            return false
        }

        return true
    }



    // ---------------------------------------------------------
    // PROCESS
    // ---------------------------------------------------------

    internal open fun alive(pid: Long): Boolean =
        ProcessHandle.of(pid).map { it.isAlive }.orElse(false)

    private fun groupPath(pid: Long) =
        "$appRootPath/$appPrefix-ffmpeg-$pid"

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
        val raw = fs.readText("${getRootPath()}/cpuset.cpus.effective")
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
                ?: fs.readText("${getRootPath()}/cpu.max")
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
        globalPinnedCores?.let { return it }          // 1. global
        manuallyPinned[pid]?.let { return it }        // 2. per-PID
        val existing = assignedCores[pid]
        val prev = assignedPercent[pid]
        return if (existing != null && prev == percent) existing
        else assignCpuset(pid, percent)               // 3. implicit
    }


    private fun initCpuset(gPath: String) {
        val mems =
            fs.readText("$appRootPath/cpuset.mems")?.trim()?.ifBlank { null }
                ?: fs.readText("${getRootPath()}/cpuset.mems")?.trim()
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
        manuallyPinned.remove(pid)

        if (assignedCores.isEmpty() && assignedPercent.isEmpty() && manuallyPinned.isEmpty() && globalPinnedCores == null) {
            synchronized(coreLock) {
                if (assignedCores.isEmpty()) {
                    nextCore = 0
                }
            }
        }

        if (!fs.exists(gPath)) return

        try {
            if (!original.isNullOrBlank() && alive(pid)) {
                val target = "${getRootPath()}/${original.removePrefix("/")}"
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