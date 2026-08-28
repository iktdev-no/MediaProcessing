package no.iktdev.mediaprocessing.shared.common.storage

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import no.iktdev.mediaprocessing.shared.common.dto.DiskInfo
import no.iktdev.mediaprocessing.shared.common.getDiskInfoFor
import org.springframework.stereotype.Service

@Service
class StorageAllocatorService(
    private val mediaPaths: MediaPaths,
) : IStorageAllocation {
    val log = KotlinLogging.logger {}


    private val stores = mutableMapOf<StorageLocation, DiskInfo>()

    @PostConstruct
    fun initialize() {
        loadStore(
            location = StorageLocation.Inbox,
            path = mediaPaths.inbox,
        )

        loadStore(
            location = StorageLocation.Scratch,
            path = mediaPaths.scratch,
        )

        loadStore(
            location = StorageLocation.Intermediate,
            path = mediaPaths.intermediate,
        )

        loadStore(
            location = StorageLocation.Outbox,
            path = mediaPaths.outbox,
        )
    }

    private fun loadStore(
        location: StorageLocation,
        path: String,
    ) {
        val diskInfo = getDiskInfoFor(path)

        if (diskInfo == null) {
            log.error {
                "Unable to determine storage information for $location at path: $path"
            }
            return
        }

        stores[location] = diskInfo

        log.debug {
            "Storage $location initialized: " +
                    "device=${diskInfo.device}, " +
                    "mount=${diskInfo.mount}, " +
                    "total=${diskInfo.totalBytes}, " +
                    "free=${diskInfo.freeBytes}"
        }
    }

    override fun getStorageInfo(path: String): DiskInfo? {
        return getDiskInfoFor(path)
    }

    fun getStorageInfo(location: StorageLocation): DiskInfo? {
        return requireNotNull(stores[location])
    }

    override fun isStoreOnSameDisk(
        disk1: DiskInfo,
        disk2: DiskInfo,
    ): Boolean {
        return disk1.device == disk2.device &&
                disk1.totalBytes == disk2.totalBytes
    }

    override fun getStorageArea(path: String): StorageLocation {
        val target = IFile(path).toPath().normalize()

        return stores
            .entries
            .firstOrNull { (_, disk) ->
                target.startsWith(IFile(disk.mount).toPath().normalize())
            }
            ?.key
            ?: throw IllegalArgumentException(
                "Path does not belong to a known storage area: $path"
            )
    }

    private fun calculateRequiredBytes(
        sourceFile: IFile,
        location: StorageLocation,
    ): Long {
        val multiplier = when (location) {
            StorageLocation.Intermediate -> 1.5
            else -> 1.0
        }

        return (sourceFile.length() * multiplier).toLong()
    }

    override fun canAllocate(
        sourceFile: IFile,
        source: StorageLocation,
        destination: StorageLocation,
    ): Boolean {
        val sourceStore = getStorageInfo(source) ?: return true
        val destinationStore = getStorageInfo(destination) ?: return true

        val sameDisk = isStoreOnSameDisk(
            sourceStore,
            destinationStore,
        )

        val requiredBytes = if (destination == StorageLocation.Scratch) {
            if (sameDisk) {
                calculateRequiredBytes(sourceFile, StorageLocation.Scratch) +
                        calculateRequiredBytes(sourceFile, StorageLocation.Intermediate)
            } else {
                calculateRequiredBytes(sourceFile, StorageLocation.Scratch)
            }
        } else {
            calculateRequiredBytes(sourceFile, destination)
        }

        val currentStore =
            getStorageInfo(destinationStore.mount)
                ?: return false

        return currentStore.freeBytes >= requiredBytes
    }
}
