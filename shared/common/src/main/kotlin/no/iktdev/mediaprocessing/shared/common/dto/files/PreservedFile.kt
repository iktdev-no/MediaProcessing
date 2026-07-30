package no.iktdev.mediaprocessing.shared.common.dto.files

import no.iktdev.files.IFile
import java.time.Instant
import java.util.UUID

data class PreservedFileRow(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val preserved: Boolean,
    val persistedAt: Instant,
) {
    fun resolve(): IFile = IFile(filePath)
    fun toPreservedFile() = PreservedFile(
        fileName = fileName,
        filePath = filePath,
        preserved = preserved,
        persistedAt = persistedAt,
    )
}

data class PreservedFile(
    val filePath: String,
    val fileName: String,
    val preserved: Boolean,
    val persistedAt: Instant? = null,
    val usedInReferences: List<UUID> = emptyList() // Fylles inn ved behov fra Event Store
) {
    fun resolve(): IFile = IFile(filePath)
    fun withReferenceIds(ids: List<UUID>): PreservedFile {
        return this.copy(usedInReferences = ids)
    }
}