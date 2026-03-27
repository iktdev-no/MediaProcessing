package no.iktdev.mediaprocessing

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService

class MockFileSystemService : FileSystemService {

    // Controls
    var copyShouldFail = false
    var deleteShouldFail = false
    var identical = true
    var sourceExists = true

    // Tracking
    val copied = mutableListOf<Pair<IFile, IFile>>()
    val verified = mutableListOf<Pair<IFile, IFile>>()
    val deleted = mutableListOf<IFile>()

    override fun copy(source: IFile, destination: IFile) {
        copied += source to destination

        if (!sourceExists) {
            throw FileServiceException.SourceMissing(source)
        }

        if (copyShouldFail) {
            throw FileServiceException.CopyFailed(source, destination, RuntimeException("copy failed"))
        }

        // Simulate successful copy by doing nothing
    }

    override fun copyWithProgress(
        source: IFile,
        destination: IFile,
        bufferSize: Int,
        onProgress: (copied: Long, total: Long) -> Unit
    ) {
        // Do Nothing for now
    }

    override fun verifyIdentical(original: IFile, target: IFile) {
        verified += original to target

        if (!identical) {
            throw FileServiceException.VerificationFailed(original, target)
        }
    }

    override fun delete(file: IFile) {
        if (deleteShouldFail) {
            throw RuntimeException("delete failed")
        }
        deleted += file
    }
}
