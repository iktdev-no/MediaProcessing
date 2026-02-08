package no.iktdev.mediaprocessing.coordinator.util

import java.io.File

interface FileSystemService {
    fun copy(source: File, destination: File)
    fun copyWithProgress(
        source: File,
        destination: File,
        bufferSize: Int = 1024 * 1024,
        onProgress: (copied: Long, total: Long) -> Unit
    )
    fun verifyIdentical(original: File, target: File)
    fun delete(file: File)
}

sealed class FileServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    class SourceMissing(val source: File) :
        FileServiceException("Source file does not exist: ${source.absolutePath}")

    class DestinationExistsButDifferent(val source: File, val destination: File) :
        FileServiceException("Destination exists but differs: ${destination.absolutePath}")

    class CopyFailed(val source: File, val destination: File, cause: Throwable?) :
        FileServiceException("Failed to copy ${source.absolutePath} → ${destination.absolutePath}", cause)

    class VerificationFailed(val source: File, val destination: File) :
        FileServiceException("Copied file is not identical: ${destination.absolutePath}")
}

