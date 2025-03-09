package no.iktdev.mediaprocessing.coordinator.watcher

import dev.vishna.watchservice.KWatchEvent.Kind.Deleted
import dev.vishna.watchservice.KWatchEvent.Kind.Initialized
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.iktdev.eventi.database.executeWithResult
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.coordinator.*
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.contract.ProcessType
import no.iktdev.mediaprocessing.shared.common.database.tables.files
import no.iktdev.mediaprocessing.shared.common.extended.isSupportedVideoFile
import no.iktdev.mediaprocessing.shared.common.ifNotEmpty
import no.iktdev.mediaprocessing.shared.common.md5
import no.iktdev.streamit.library.db.executeOrException
import no.iktdev.streamit.library.db.withTransaction
import org.jetbrains.exposed.sql.insertIgnore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import javax.annotation.PreDestroy


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
    val watchDirectories = SharedConfig.incomingContent
    val queue = FileWatcherQueue()

    private var isStopping: Boolean = false
    @PreDestroy
    fun setStop() {
        isStopping = true
    }


    suspend fun watchFiles() {
        log.info { "Starting Watcher" }
        val dirs = watchDirectories.filter { it.exists() && it.isDirectory }
        if (dirs.isNotEmpty()) {
            val paths = dirs.joinToString("\n\t") { it.absolutePath }
            log.info { "Watching directories:\n\t$paths" }

            for (dir in dirs) {
                val files = dir.listFiles()?.map { it.name }?.joinToString { "\n\t $it" } ?: "No files present.."
                log.info { "Content present for path ${dir.absolutePath} \n$files" }
            }
        }

        //val errorConfiguredDirs = watchDirectories.filter { !it.isDirectory || !it.exists()}.joinToString("\n\t") { it.absolutePath }
        watchDirectories.filter { !it.isDirectory || !it.exists()}.ifNotEmpty {
            val errorConfiguredDirs = it.joinToString("\n\t") { it.absolutePath }
            log.error { "Failed to initialize watcher for the following: \n\t $errorConfiguredDirs" }
        }
        for (folder in watchDirectories) {
            startWatchOnDirectory(folder)
        }
    }

    val activeWatchers: MutableList<FileWatcher> = mutableListOf()
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun startWatchOnDirectory(file: File) {
        if (activeWatchers.any {it -> it.file.absolutePath == file.absolutePath}) {
            log.error { "Attempting to start a watcher on an already watched directory ${file.absolutePath}" }
        }
        val watcher =  file.asWatcher { watcher ->
            watcher.consumeEach {
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
        }.also { watcher ->
            watcher.watcher.invokeOnClose {
                it?.printStackTrace()
                log.warn { "Watcher stopped for ${watcher.file}" }
                if (!isStopping) {
                    log.info { "Determined that the program is not in a termination stage.. Restarting watcher" }
                    activeWatchers.remove(watcher)
                    ioCoroutine.launch {
                        log.info { "Waiting 500ms before restarting watcher.." }
                        delay(500)
                        startWatchOnDirectory(watcher.file)
                    }
                }
            }
        }
        log.info { "Now watching ${file.absolutePath} for files" }
        activeWatchers.add(watcher)
    }


    init {
        ioCoroutine.launch {
            watchFiles()
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
        log.info { "Removing file from Queue ${file.name}" }
        queue.removeFromQueue(file, this@InputDirectoryWatcher::onFileRemoved)
    }

    override fun onFileAvailable(file: PendingFile) {
        logger.info { "File available ${file.file.name}" }

        // This sends it to coordinator to start the process
        executeWithResult(eventDatabase.database.database) {
            files.insertIgnore {
                it[baseName] = file.file.nameWithoutExtension
                it[folder] = file.file.parentFile.absolutePath
                it[fileName] = file.file.absolutePath
                it[checksum] = file.file.md5()
            }
        }
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