package no.iktdev.mediaprocessing.coordinator.util

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths.get

data class DiskInfo(
    val mount: String,
    val device: String,
    val totalBytes: Long,
    val freeBytes: Long
)

fun getDiskInfoFor(mounts: List<String>): List<DiskInfo> {
    val fileStores = FileSystems.getDefault().fileStores

    return mounts.mapNotNull { mount ->
        val path = get(mount)

        val store = fileStores.find { fs ->
            try {
                Files.getFileStore(path) == fs
            } catch (e: Exception) {
                false
            }
        } ?: return@mapNotNull null

        DiskInfo(
            mount = mount,
            device = store.name(),
            totalBytes = store.totalSpace,
            freeBytes = store.usableSpace
        )
    }
}
