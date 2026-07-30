package no.iktdev.mediaprocessing.shared.common.files

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.dto.files.PreservedFile
import kotlin.time.Instant

abstract class FilePreservationImpl(private val store: IFilePreservationStore) {

    open fun getPreservedFiles(): List<PreservedFile> {
        return store.getAllPreserved()
    }

    open fun getAllFiles() = store.getAll()


    open fun addFile(file: IFile) {
        store.persist(fileName = file.name, filePath = file.absolutePath)
    }

    open fun removeFile(file: IFile) {
        store.deleteByFileName(file.name)
    }

    open fun setPreservationStatus(preserve: Boolean, file: IFile) {
        if (preserve) {
            // this will update path and set preserved to true
            store.persist(fileName = file.name, filePath = file.absolutePath)
        } else {
            store.setPreservedStatus(fileName = file.name, preserve)
        }
    }

    open fun preserveFiles(files: List<IFile>) {
        store.persist(files.map { it.name to it.absolutePath })
    }

}