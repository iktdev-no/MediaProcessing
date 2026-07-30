package no.iktdev.mediaprocessing.shared.common.files

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.dto.files.PreservedFile

interface IFilePreservationStore {
    fun persist(fileName: String, filePath: String): Boolean
    fun persist(fi: List<Pair<String, String>>)
    fun delete(id: Long)
    fun deleteByFileName(fileName: String)
    fun deleteByFilePath(filePath: String)
    fun setPreservedStatus(id: Long, preserved: Boolean)
    fun setPreservedStatus(fileName: String, preserved: Boolean): Boolean
    fun getByPreserved(preserved: Boolean): List<PreservedFile>
    fun getAll(): List<PreservedFile>
    fun getById(id: Long): PreservedFile?
    fun getByFileName(fileName: String): PreservedFile?
    fun getAllPreserved(): List<PreservedFile>
}