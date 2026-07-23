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
        // 1. Hurtigsjekk på størrelse først.
        // Hvis størrelsen er ulik, kan de umulig være identiske.
        if (original.length() != target.length()) {
            throw FileServiceException.VerificationFailed(original, target)
        }

        try {
            // 2. Prøv den raske standardmetoden (Files.mismatch)
            val mismatch = Files.mismatch(original.toPath(), target.toPath())
            if (mismatch == -1L) {
                return true
            }
        } catch (e: Exception) {
            // Ignorer og gå videre til fallback hvis Files.mismatch feiler (f.eks. pga låste filer el.)
        }

        // 3. FALLBACK: Hvis mismatch fant ulikheter (eller feilet),
        // bruker vi xxHash for å sjekke om innholdet likevel er 100% likt.
        val srcHash = original.toXxHash()
        val dstHash = target.toXxHash()

        if (srcHash == dstHash) {
            return true
        }

        // Hvis hashen heller ikke matcher, er filene faktisk forskjellige
        throw FileServiceException.VerificationFailed(original, target)
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

