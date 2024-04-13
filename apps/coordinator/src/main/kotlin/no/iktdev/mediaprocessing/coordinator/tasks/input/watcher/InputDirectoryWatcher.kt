package no.iktdev.mediaprocessing.coordinator.tasks.input.watcher

import dev.vishna.watchservice.KWatchEvent.Kind.Deleted
import dev.vishna.watchservice.KWatchEvent.Kind.Initialized
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.log
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.extended.isSupportedVideoFile
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File


interface FileWatcherEvents {
    fun onFileAvailable(file: PendingFile)

    /**
     * If the file is being copied or incomplete, or in case a process currently owns the file, pending should be issued
     */
    fun onFilePending(file: PendingFile)

    /**
     * If the file is either removed or is not a valid file
     */
    fun onFileFailed(file: PendingFile)


    fun onFileRemoved(file: PendingFile)
}



@Service
class InputDirectoryWatcher(@Autowired var coordinator: Coordinator): FileWatcherEvents {
    private val logger = KotlinLogging.logger {}
    val watcherChannel = SharedConfig.incomingContent.asWatchChannel()
    val queue = FileWatcherQueue()
    val io = Coroutines.io()

    init {
        io.launch {
            watcherChannel.consumeEach {
                if (it.file == SharedConfig.incomingContent) {
                    logger.info { "IO Watcher ${it.kind} on ${it.file.absolutePath}" }
                } else {
                    logger.info { "IO Event: ${it.kind}: ${it.file.name}" }
                }
                try {
                    when (it.kind) {
                        Deleted -> removeFile(it.file)
                        Initialized -> { /* Do nothing */ }
                        else -> {
                            val added = addFile(it.file)
                            if (!added) {
                                logger.info { "Ignoring event kind: ${it.kind.name} for file ${it.file.name} as it is not a supported video file" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun addFile(file: File): Boolean {
        return if (file.isFile && file.isSupportedVideoFile()) {
            log.info { "Adding ${file.name} to queue" }
            queue.addToQueue(file, this@InputDirectoryWatcher::onFilePending, this@InputDirectoryWatcher::onFileAvailable)
            true
        } else if (file.isDirectory) {
            log.info { "Searching for files in ${file.name}" }
            val supportedFiles = file.walkTopDown().filter { f -> f.isFile && f.isSupportedVideoFile() }
            supportedFiles.forEach { sf ->
                log.info { "Adding ${sf.name} to queue from folder" }
                queue.addToQueue(sf, this@InputDirectoryWatcher::onFilePending, this@InputDirectoryWatcher::onFileAvailable)
            }
            true
        } else false
    }

    private fun removeFile(file: File) {
        queue.removeFromQueue(file, this@InputDirectoryWatcher::onFileRemoved)
    }

    override fun onFileAvailable(file: PendingFile) {
        logger.info { "File available ${file.file.name}" }

        // This sends it to coordinator to start the process
        coordinator.startProcess(file.file, ProcessType.FLOW)
    }

    override fun onFilePending(file: PendingFile) {
        logger.info { "File pending availability ${file.file.name}" }
    }

    override fun onFileFailed(file: PendingFile) {
        logger.warn { "File failed availability ${file.file.name}" }
    }

    override fun onFileRemoved(file: PendingFile) {
        logger.info { "File removed ${file.file.name} was removed" }
    }

}