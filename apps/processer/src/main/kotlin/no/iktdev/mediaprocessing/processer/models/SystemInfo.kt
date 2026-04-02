package no.iktdev.mediaprocessing.processer.models

data class SystemInfo(
    val cpuModel: String?,
    val cpuCores: Int,
    val cpuThreads: Int,
    val loadAvg: Triple<Double, Double, Double>,
    val uptimeSeconds: Long,
    val totalMemKb: Long,
    val freeMemKb: Long,
    val availableMemKb: Long,
    val swapTotalKb: Long,
    val swapFreeKb: Long,
    val cpuFrequencies: Map<Int, Int>,   // cpu -> MHz
    val temperatures: Map<String, Double> // sensor -> Celsius
)
