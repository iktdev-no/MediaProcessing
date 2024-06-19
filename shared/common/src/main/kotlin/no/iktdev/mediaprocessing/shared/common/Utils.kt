package no.iktdev.mediaprocessing.shared.common

import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress

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

fun List<PersistentMessage>.lastOrSuccess(): PersistentMessage? {
    return this.lastOrNull { it.data.isSuccess() } ?: this.lastOrNull()
}

fun List<PersistentMessage>.lastOrSuccessOf(event: KafkaEvents): PersistentMessage? {
    val validEvents = this.filter { it.event == event }
    return validEvents.lastOrNull { it.data.isSuccess() } ?: validEvents.lastOrNull()
}

fun List<PersistentMessage>.lastOrSuccessOf(event: KafkaEvents, predicate: (PersistentMessage) -> Boolean): PersistentMessage? {
    val validEvents = this.filter { it.event == event && predicate(it) }
    return validEvents.lastOrNull()
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
    val netHostname = try {
        val host = InetAddress.getLocalHost()
        listOf(host.hostName, host.canonicalHostName)
    } catch (e: Exception) {
        emptyList<String>()
    }.filterNot { it.isNullOrBlank() }

    val envs = listOfNotNull(
        System.getenv("hostname"),
        System.getenv("computername")
    )

    return (envs + netHostname).firstOrNull() ?: "UNKNOWN_SYSTEM"
}

fun silentTry(code: () -> Unit) {
    try {
        code.invoke()
    } catch (_: Exception) {}
}