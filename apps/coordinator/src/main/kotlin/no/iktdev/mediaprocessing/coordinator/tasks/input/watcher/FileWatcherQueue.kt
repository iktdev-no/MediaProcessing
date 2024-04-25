package no.iktdev.mediaprocessing.coordinator.tasks.input.watcher

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.coordinator.defaultCoroutine
import no.iktdev.mediaprocessing.shared.common.isFileAvailable
import java.io.File
import java.util.UUID

data class PendingFile(val id: String = UUID.randomUUID().toString(), val file: File, var time: Long = 0)
class FileWatcherQueue {
    private val fileChannel = Channel<PendingFile>()

    fun addToQueue(file: File, onFilePending: (PendingFile) -> Unit, onFileAccessible: (PendingFile) -> Unit) {
        // Check if the file is accessible
        if (isFileAvailable(file)) {
            // If accessible, run the function immediately and return
            onFileAccessible(PendingFile(file = file))
            return
        }

        // Add the file to the channel for processing
        fileChannel.trySend(PendingFile(file = file))

        // Coroutine to process the file and remove it from the queue when accessible
        defaultCoroutine.launch {
            while (true) {
                delay(500)
                val currentFile = fileChannel.receive()
                if (isFileAvailable(currentFile.file)) {
                    onFileAccessible(currentFile)
                    // File is accessible, remove it from the queue
                    removeFromQueue(currentFile.file) { /* Do nothing here as the operation is not intended to be performed here */ }
                } else {
                    // File is not accessible, put it back in the channel for later processing
                    fileChannel.send(currentFile.apply { time += 500 })
                    onFilePending(currentFile)
                }
            }
        } // https://chat.openai.com/share/f3c8f6ea-603a-40d6-a811-f8fea5067501
    }

    fun removeFromQueue(file: File, onFileRemoved: (PendingFile) -> Unit) {
        val currentItems = fileChannel.list()
        val toRemove = currentItems.filter {
            if (it.file.isDirectory) it.file.name == file.name else it.file.name == file.name && it.file.parent == file.parent
        }

        toRemove.let {
            it.forEach { file -> onFileRemoved(file) }
        }
    }


    // Extension function to find and remove an element from the channel
    fun <T> Channel<T>.findAndRemove(predicate: (T) -> Boolean): List<T> {
        val forRemoved = mutableListOf<T>()
        val items = mutableListOf<T>()
        while (true) {
            val item = tryReceive().getOrNull() ?: break
            if (predicate(item)) {
                forRemoved.add(item)
            }
            items.add(item)
        }
        for (item in items) {
            trySend(item).isSuccess
        }
        return forRemoved
    }

    fun <T> Channel<T>.list(): List<T> {
        val items = mutableListOf<T>()
        while (true) {
            val item = tryReceive().getOrNull() ?: break
            items.add(item)
            trySend(item).isSuccess
        }
        return items
    }


}