package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files

class DefaultFileSystemService : FileSystemService {

    override fun copy(source: File, destination: File) {
        if (!source.exists()) {
            throw FileServiceException.SourceMissing(source)
        }

        try {
            source.copyTo(destination, overwrite = true)
        } catch (e: Exception) {
            throw FileServiceException.CopyFailed(source, destination, e)
        }
    }

    override fun verifyIdentical(source: File, destination: File) {
        val mismatch = Files.mismatch(source.toPath(), destination.toPath())
        if (mismatch != -1L) {
            throw FileServiceException.VerificationFailed(source, destination)
        }
    }

    override fun delete(file: File) {
        file.delete()
    }

    override fun copyWithProgress(
        source: File,
        destination: File,
        bufferSize: Int,
        onProgress: (copied: Long, total: Long) -> Unit
    ) {
        if (!source.exists()) {
            throw FileServiceException.SourceMissing(source)
        }

        val totalBytes = source.length()
        var copied = 0L

        try {
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(bufferSize)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress(copied, totalBytes)
                    }
                }
            }
        } catch (e: Exception) {
            throw FileServiceException.CopyFailed(source, destination, e)
        }
    }
}

