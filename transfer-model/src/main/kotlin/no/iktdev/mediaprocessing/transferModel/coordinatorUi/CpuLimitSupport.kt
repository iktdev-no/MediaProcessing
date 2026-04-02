package no.iktdev.mediaprocessing.transferModel.coordinatorUi


enum class OperatingSystem {
    LINUX,
    WINDOWS,
    MACOS,
    OTHER
}

abstract class CpuLimitSupport(
    open val os: OperatingSystem,
    open val supported: Boolean,
    val reason: String? = null,
) {
}
class LinuxCpuLimitSupport(
    os: OperatingSystem = OperatingSystem.LINUX,
    supported: Boolean,
    reason: String? = null,
    val cgroupV2: Boolean = false,
    val cpuController: Boolean = false,
    val cpusetController: Boolean = false,
    val cgroupMounted: Boolean = false,
    val subtreeControlExists: Boolean = false,
): CpuLimitSupport(os = os, supported = supported, reason = reason) {}