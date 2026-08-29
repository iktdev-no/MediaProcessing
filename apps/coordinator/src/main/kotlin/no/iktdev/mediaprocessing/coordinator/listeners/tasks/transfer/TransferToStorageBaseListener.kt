package no.iktdev.mediaprocessing.coordinator.listeners.tasks.transfer

import mu.KotlinLogging
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.files.FileHash
import no.iktdev.files.FileHashType
import no.iktdev.files.IFile
import no.iktdev.files.ensureParentDirsExist
import no.iktdev.mediaprocessing.coordinator.services.DefaultFileSystemService
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.Overrides
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

    fun transfer(source: SourceFile, destination: DestinationFile, overrides: List<Overrides> = emptyList(),
                 onProgress: ((copied: Long, total: Long) -> Unit)? = null) {
        val src = source.file
        val dst = destination.destination
        if (src.notExist() && dst.exists()) {
            val dstHash = if (source.hash != null) {
                when (source.hash.method) {
                    FileHashType.SHA256 -> dst.toSha256()
                    FileHashType.XX64Hash -> dst.toXxHash()
                }
            } else null
            if (source.hash?.hash != dstHash?.hash) {
                throw FileServiceException.SourceMissingDestinationHashMismatch(src, source.hash, dst, dstHash)
            }
            throw FileServiceException.SourceAlreadyTransferred(src, dst)
        }
        val fs = getFileSystemService()
        if (dst.exists() && overrides.none { it == Overrides.AllowOverwrite }) {
            try {
                val ok = fs.verifyIdentical(src, dst)
                if (ok) {
                    throw FileServiceException.FilesAreIdentical(src, dst)
                }
            } catch (e: FileServiceException.VerificationFailed) {
                e.printStackTrace()
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

    class SourceFile(val uri: String, val hash: FileHash? = null) {
        val file = IFile(uri)
    }

    class DestinationFile(val uri: String) {
        val destination = IFile(this.uri)
        init {
            destination.ensureParentDirsExist()
        }
    }
}