package no.iktdev.mediaprocessing.shared.common

import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.io.File
import java.io.RandomAccessFile

private val logger = KotlinLogging.logger {}

fun isFileAvailable(file: File): Boolean {
    if (!file.exists()) return false
    var stream: RandomAccessFile? = null
    try {
        stream = RandomAccessFile(file, "rw")
        stream.close()
        logger.info { "File ${file.name} is read and writable" }
        return true
    } catch (e: Exception) {
        stream?.close()
    }
    return false
}


suspend fun limitedWhile(condition: () -> Boolean, maxDuration: Long = 500 * 60, delayed: Long = 500, block: () -> Unit) {
    var elapsedDelay = 0L
    do {
        block.invoke()
        elapsedDelay += delayed
        delay(delayed)
    } while (condition.invoke() && elapsedDelay < maxDuration)
}

fun getComputername(): String {
    return listOfNotNull(
        System.getenv("hostname"),
        System.getenv("computername")
    ).firstOrNull() ?: "UNKNOWN_SYSTEM"
}