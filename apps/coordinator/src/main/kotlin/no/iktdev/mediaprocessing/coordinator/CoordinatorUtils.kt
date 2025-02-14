package no.iktdev.mediaprocessing.coordinator

import dev.vishna.watchservice.KWatchChannel
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.runBlocking
import java.io.File

data class FileWatcher(val file: File, val watcher: KWatchChannel)

suspend fun File.asWatcher(block: suspend (KWatchChannel) -> Unit): FileWatcher {
    val channel = this.asWatchChannel()
    block(channel)
    return FileWatcher(this, channel)
}