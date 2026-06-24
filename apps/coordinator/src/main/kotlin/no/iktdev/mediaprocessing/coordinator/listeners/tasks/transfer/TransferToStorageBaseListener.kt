package no.iktdev.mediaprocessing.coordinator.listeners.tasks.transfer

import mu.KotlinLogging
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.services.DefaultFileSystemService
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks_super.TransferTask
import no.iktdev.mediaprocessing.shared.common.silentTry
import java.nio.file.FileSystemException

abstract class TransferToStorageBaseListener(val deleteSourceAfterVerify: Boolean = true): TaskListener(TaskType.IO_INTENSIVE) {
    val log = KotlinLogging.logger {}

    open fun getFileSystemService(): FileSystemService =
        DefaultFileSystemService()


    private fun deleteCache(fs: FileSystemService, source: SourceFile) {
        source.file.let { silentTry { fs.delete(it) } }
    }

    fun transfer(source: SourceFile, destination: DestinationFile, overrides: List<TransferTask.Overrides> = emptyList(),
                 onProgress: ((copied: Long, total: Long) -> Unit)? = null) {
        val src = source.file
        val dst = destination.destination
        val fs = getFileSystemService()
        if (dst.exists() && overrides.none { it == TransferTask.Overrides.AllowOverwrite }) {
            try {
                val ok = fs.verifyIdentical(src, dst)
                if (ok) {
                    throw FileServiceException.FilesAreIdentical(src, dst)
                }
            } catch (e: FileServiceException.VerificationFailed) {
                throw FileServiceException.DestinationExistsButDifferent(src, dst)
            }
        }

        if (onProgress != null) {
            fs.copyWithProgress(source = src, destination = dst, onProgress = onProgress)
        } else {
            fs.copy(source = src, destination = dst)
        }
        val ok = fs.verifyIdentical(src, dst)
        if (ok && deleteSourceAfterVerify) {
            deleteCache(fs, source)
        }
    }

    class SourceFile(val uri: String) {
        val file = IFile(uri)
        init {
            if (!file.exists()) {
                throw FileServiceException.SourceMissing(file)
            }
        }
    }

    class DestinationFile(val uri: String) {
        val destination = IFile(this.uri)
        init {
            if (!destination.parentFile.exists()) {
                if (!destination.parentFile.mkdirs()) {
                    throw FileSystemException("Failed to create directory: ${destination.absolutePath}")
                }
            }
        }
    }
}