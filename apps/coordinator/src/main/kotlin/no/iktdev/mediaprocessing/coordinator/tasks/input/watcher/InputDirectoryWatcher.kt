package no.iktdev.mediaprocessing.coordinator.tasks.input.watcher

import dev.vishna.watchservice.KWatchEvent.Kind.Deleted
import dev.vishna.watchservice.KWatchEvent.Kind.Initialized
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.extended.isSupportedVideoFile
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service



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
                when (it.kind) {
                    Deleted -> queue.removeFromQueue(it.file, this@InputDirectoryWatcher::onFileRemoved)
                    Initialized -> { /* Do nothing */ }
                    else -> {
                        if (it.file.isFile && it.file.isSupportedVideoFile()) {
                            queue.addToQueue(it.file, this@InputDirectoryWatcher::onFilePending, this@InputDirectoryWatcher::onFileAvailable)
                        } else if (it.file.isDirectory) {
                            val supportedFiles = it.file.walkTopDown().filter { f -> f.isFile && f.isSupportedVideoFile() }
                            supportedFiles.forEach { sf ->
                                queue.addToQueue(sf, this@InputDirectoryWatcher::onFilePending, this@InputDirectoryWatcher::onFileAvailable)
                            }
                        } else {
                            logger.info { "Ignoring event kind: ${it.kind.name} for file ${it.file.name} as it is not a supported video file" }
                        }
                    }
                }
            }
        }
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