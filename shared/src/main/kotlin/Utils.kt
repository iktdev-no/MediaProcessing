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