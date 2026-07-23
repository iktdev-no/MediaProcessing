package no.iktdev.mediaprocessing.coordinator.util

import no.iktdev.files.FileHash
import no.iktdev.files.IFile

interface FileSystemService {
    fun copy(source: IFile, destination: IFile)
    fun copyWithProgress(
        source: IFile,
        destination: IFile,
        bufferSize: Int = 1024 * 1024,
        onProgress: (copied: Long, total: Long) -> Unit
    )
    fun verifyIdentical(original: IFile, target: IFile): Boolean
    fun delete(file: IFile)
}

sealed class FileServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    class SourceMissing(val source: IFile) :
        FileServiceException("Source file does not exist: ${source.absolutePath}")

    class DestinationExistsButDifferent(val source: IFile, val destination: IFile) :
        FileServiceException("Destination exists but differs: ${destination.absolutePath}, source file: ${source.absolutePath}")

    class CopyFailed(val source: IFile, val destination: IFile, cause: Throwable?) :
        FileServiceException("Failed to copy ${source.absolutePath} → ${destination.absolutePath}", cause)

    class VerificationFailed(val source: IFile, val destination: IFile) :
        FileServiceException("Copied file is not identical: ${destination.absolutePath}")

    class FilesAreIdentical(val source: IFile, val destination: IFile) : FileServiceException("Files are identical: ${source.absolutePath} -> ${destination.absolutePath}. No need to copy")
    class SourceAlreadyTransferred(val source: IFile, val destination: IFile) : FileServiceException("Source file: ${source.absolutePath}  is missing, but its in destination ${destination.absolutePath}. Unable to transfer. System probably failed to persist event with result")
    class SourceMissingDestinationHashMismatch(
        val source: IFile,
        val sourceHash: FileHash?,
        val destination: IFile,
        val destinationHash: FileHash?
    ) : FileServiceException(
        "Source '${source.absolutePath}' is missing, but destination '${destination.absolutePath}' exists with a non-matching hash. " +
                "Expected ${sourceHash?.method}:${sourceHash?.hash}, got ${destinationHash?.method}:${destinationHash?.hash}. Transfer cannot continue."
    )

}

