package no.iktdev.mediaprocessing.shared.common.storage

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.dto.DiskInfo

interface IStorageAllocation {

    fun getStorageInfo(path: String): DiskInfo?
    fun isStoreOnSameDisk(disk1: DiskInfo, disk2: DiskInfo): Boolean
    fun getStorageArea(path: String): StorageLocation

    fun canAllocate(
        sourceFile: IFile,
        source: StorageLocation,
        destination: StorageLocation,
    ): Boolean
}