package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import java.nio.file.Files

class DefaultFileSystemService : FileSystemService {

    override fun copy(source: IFile, destination: IFile) {
        if (!source.exists()) {
            throw FileServiceException.SourceMissing(source)
        }

        try {
            source.copyTo(destination, overwrite = true)
        } catch (e: Exception) {
            throw FileServiceException.CopyFailed(source, destination, e)
        }
    }

    override fun verifyIdentical(original: IFile, target: IFile): Boolean {
        val mismatch = Files.mismatch(original.toPath(), target.toPath())
        if (mismatch != -1L) {
            throw FileServiceException.VerificationFailed(original, target)
            return false
        }
        return true
    }

    override fun delete(file: IFile) {
        file.delete()
    }

    override fun copyWithProgress(
        source: IFile,
        destination: IFile,
        bufferSize: Int,
        onProgress: (copied: Long, total: Long) -> Unit
    ) {
        if (!source.exists()) {
            throw FileServiceException.SourceMissing(source)
        }

        val totalBytes = source.length()
        var copied = 0L

        try {
            source.openInputStream().use { input ->
                destination.openOutputStream().use { output ->
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

