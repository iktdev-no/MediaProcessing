package no.iktdev.mediaprocessing.shared.common.dto.files

import no.iktdev.files.FileHash
import no.iktdev.files.IFile
import no.iktdev.files.IFile.Companion.invoke

data class HashedFile(
    val absoluteFilePath: String,
    val hashedFile: FileHash? = null
) {
    fun deconstruct(): Pair<IFile, FileHash?> {
        return IFile(absoluteFilePath) to hashedFile
    }
}