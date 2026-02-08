package no.iktdev.mediaprocessing

import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import java.io.File

class MockFileSystemService : FileSystemService {

    // Controls
    var copyShouldFail = false
    var deleteShouldFail = false
    var identical = true
    var sourceExists = true

    // Tracking
    val copied = mutableListOf<Pair<File, File>>()
    val verified = mutableListOf<Pair<File, File>>()
    val deleted = mutableListOf<File>()

    override fun copy(source: File, destination: File) {
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
        source: File,
        destination: File,
        bufferSize: Int,
        onProgress: (copied: Long, total: Long) -> Unit
    ) {
        // Do Nothing for now
    }

    override fun verifyIdentical(source: File, destination: File) {
        verified += source to destination

        if (!identical) {
            throw FileServiceException.VerificationFailed(source, destination)
        }
    }

    override fun delete(file: File) {
        if (deleteShouldFail) {
            throw RuntimeException("delete failed")
        }
        deleted += file
    }
}
