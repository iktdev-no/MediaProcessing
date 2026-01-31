package no.iktdev.mediaprocessing.coordinator.util

import java.nio.file.Files
import java.nio.file.Paths

data class DiskInfo(
    val mount: String,
    val device: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usedPercent: Double
)


fun getDiskInfoFor(mounts: List<String>): List<DiskInfo> =
    mounts.mapNotNull { mount ->
        val path = Paths.get(mount)

        val store = runCatching { Files.getFileStore(path) }.getOrNull()
            ?: return@mapNotNull null

        DiskInfo(
            mount = mount,
            device = store.name(),
            totalBytes = store.totalSpace,
            freeBytes = store.usableSpace,
            usedBytes = store.totalSpace - store.usableSpace,
            usedPercent = if (store.totalSpace > 0)
                ((store.totalSpace - store.usableSpace).toDouble() / store.totalSpace.toDouble()) * 100
            else 0.0
        )
    }
