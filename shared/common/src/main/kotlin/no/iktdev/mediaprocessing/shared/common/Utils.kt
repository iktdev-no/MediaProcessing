package no.iktdev.mediaprocessing.shared.common

import com.ibm.icu.text.Transliterator
import kotlinx.coroutines.delay
import mu.KotlinLogging
import no.iktdev.eventi.events.SoftDispatchException
import no.iktdev.eventi.models.DeleteEvent
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.serialization.ZDS.toEvent
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.net.InetAddress
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.CRC32
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

fun File.notExist(): Boolean {
    return !this.exists()
}

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

fun getAppVersion(): Int {
    val parsed = System.getenv("APP_VERSION")?.let {
        Regex("[^0-9]").replace(it, "")
    } ?: "100"
    return Integer.parseInt(parsed)
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

fun File.getCRC32(): Long {
    val crc = CRC32()
    this.inputStream().use { input ->
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            crc.update(buffer, 0, bytesRead)
        }
    }
    return crc.value
}

fun File.moveTo(destinationFile: File, onProgress: (Double) -> Unit = {}): Boolean {
    assert(this.exists()) {
        "Sourcefile ${this.absolutePath} does not exist, but it should"
    }
    assert(destinationFile.notExist()) {
        "Destinationfile ${destinationFile.absolutePath} exists, but it shouldn't"
    }
    val tempDestinationFile = File(destinationFile.parentFile, "${destinationFile.name}.tmp")


    val success: Boolean = run {
        try {
            val totalBytes = this.length()
            var copiedBytes = 0L

            this.inputStream().use { input ->
                tempDestinationFile.outputStream().use { output ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        copiedBytes += bytesRead
                        onProgress(copiedBytes.toDouble() / totalBytes * 100)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    if (!success) {
        return false
    }

    val sourceHash = this.getCRC32()
    val tempFileHash = tempDestinationFile.getCRC32()

    if (sourceHash == tempFileHash) {
        if (!tempDestinationFile.renameTo(destinationFile)) {
            logger.error { "${tempDestinationFile.name} failed to rename to ${destinationFile.name}" }
            return false
        }
        this.delete()
    } else {
        logger.error { "${tempDestinationFile.name} failed integrity check" }
        return false
    }

    return true
}

fun <T> List<T>.ifNotEmpty(block: (List<T>) -> Unit) {
    if (this.isNotEmpty()) {
        block(this)
    }
}

fun File.md5(): String {
    return getChecksum(this.absolutePath)
}

fun getChecksum(filePath: String): String {
    val digest = MessageDigest.getInstance("MD5")
    val fis = FileInputStream(filePath)
    val byteArray = ByteArray(1024)
    var bytesCount: Int

    while (fis.read(byteArray).also { bytesCount = it } != -1) {
        digest.update(byteArray, 0, bytesCount)
    }

    fis.close()

    val bytes = digest.digest()
    val sb = StringBuilder()
    for (byte in bytes) {
        sb.append(String.format("%02x", byte))
    }
    return sb.toString()
}

inline fun <reified T> RestTemplate.tryPost(url: String, data: Any, noinline onError: ((Exception) -> Unit)? = null) {
    try {
        this.postForEntity(url, data,  T::class.java)
    } catch (e: Exception) {
        onError?.invoke(e)
    }
}

fun SimpMessagingTemplate.trySend(destination: String, data: Any, onError: ((Exception) -> Unit)? = null) {
    try {
        this.convertAndSend(destination, data)
    } catch (e: Exception) {
        onError?.invoke(e)
    }
}


inline fun <reified T : Event> List<Event>.getInstanceOf(): T? {
    return this.firstOrNull { it is T } as? T
}

// Extension-funksjon på List<Event> som returnerer alle instanser av T
inline fun <reified T : Event> List<Event>.getInstancesOf(): List<T> {
    return this.filterIsInstance<T>()
}

inline fun <reified T> List<T>.sizeEquals(other: List<T>): Boolean {
    return this.size == other.size
}

fun File.resolveConflict(): File {
    if (!exists()) return this

    val parent = parentFile
    val name = nameWithoutExtension
    val ext = extension

    var index = 1
    var candidate: File

    do {
        candidate = File(parent, "$name ($index).$ext")
        index++
    } while (candidate.exists())

    return candidate
}

fun UtcNow(): Instant = Instant.now()

fun List<Event>.effective(): List<Event> {
    val deletedIds = this
        .filterIsInstance<DeleteEvent>()
        .map { it.deletedEventId }
        .toSet()

    return this
        .filter { it.eventId !in deletedIds }
        .filterNot { it is DeleteEvent }
}

fun List<PersistedEvent>.effectivePersisted(): List<PersistedEvent> {
    val parsed = this.mapNotNull { pe ->
        pe.toEvent()?.let { ev -> pe to ev }
    }

    val effectiveEvents = parsed
        .map { it.second }
        .effective() // bruker extension over

    val effectiveIds = effectiveEvents.map { it.eventId }.toSet()

    return parsed
        .filter { (_, ev) -> ev.eventId in effectiveIds }
        .map { it.first }
        .sortedBy { it.persistedAt }
}

fun <T : Any> KClass<T>.getName(): String =
    this.simpleName ?: this.java.simpleName


private val transliterator = Transliterator.getInstance("Any-Latin; Latin-ASCII")
fun String.cleanForFileSystemUse(): String {
    // 1. Full translitterering (Æ→AE, Ø→O, Å→AA, Ł→L, Þ→Th, etc.)
    val ascii = transliterator.transliterate(this)

    // 2. Fjern alt som ikke er bokstav, tall, mellomrom, bindestrek, parentes, komma, punktum
    val cleaned = ascii.replace(Regex("[^\\p{L}\\p{N}\\s\\-(),.!]"), " ")

    // 3. Normaliser whitespace
    return cleaned.replace(Regex("\\s{2,}"), " ").trim()
}


inline fun <reified T : Event> List<Event>.requireEvent(): T {
    return this.filterIsInstance<T>().firstOrNull()
        ?: throw SoftDispatchException.MissingEventException(T::class.java)
}

inline fun <reified T : Event, R> List<Event>.requireEventValue(
    crossinline extractor: (T) -> R?
): R {
    val event = this.filterIsInstance<T>().firstOrNull()
        ?: throw SoftDispatchException.MissingEventException(T::class.java)

    return extractor(event)
        ?: throw SoftDispatchException.ForcedListenerEjectionException(
            "Missing required value in ${T::class.simpleName}",
            T::class.java
        )
}

inline fun <reified T : Event> Event.requireQualifiedEntry(): T {
    return this as? T
        ?: throw SoftDispatchException.UnqualifiedEntryEventException(T::class.java)
}



