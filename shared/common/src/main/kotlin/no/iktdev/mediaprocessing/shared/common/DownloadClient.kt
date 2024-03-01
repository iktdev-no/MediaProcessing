package no.iktdev.mediaprocessing.shared.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.iktdev.exfl.using
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

open class DownloadClient(val url: String, val outDir: File, val baseName: String) {
    val log = KotlinLogging.logger {}
    protected val http: HttpURLConnection = openConnection()
    private val BUFFER_SIZE = 4096

    private fun openConnection(): HttpURLConnection {
        try {
            return URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            e.printStackTrace()
            throw BadAddressException("Provided url is either not provided (null) or is not a valid http url")
        }
    }

    protected fun getLength(): Int {
        return http.contentLength
    }

    protected fun getProgress(read: Int, total: Int = getLength()): Int {
        return ((read * 100) / total)
    }

    suspend fun getOutFile(): File? = withContext(Dispatchers.IO) {
        val extension = getExtension()
            ?: throw UnsupportedFormatException("Provided url does not contain a supported file extension")
        val outFile = outDir.using("$baseName.$extension")
        if (!outDir.exists()) {
            log.error { "Unable to create parent folder for ${outFile.name}. Download skipped!" }
            return@withContext null
        }
        return@withContext outFile
    }

    suspend fun download(outFile: File): File? = withContext(Dispatchers.IO) {
        if (outFile.exists()) {
            log.info { "${outFile.name} already exists. Download skipped!" }
            return@withContext null
        }

        val inputStream = http.inputStream
        val fos = FileOutputStream(outFile, false)

        var totalBytesRead = 0
        val buffer = ByteArray(BUFFER_SIZE)
        inputStream.apply {
            fos.use { fout ->
                run {
                    var bytesRead = read(buffer)
                    while (bytesRead >= 0) {
                        fout.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        bytesRead = read(buffer)
                        // System.out.println(getProgress(totalBytesRead))
                    }
                }
            }
        }
        inputStream.close()
        fos.close()
        return@withContext outFile
    }

    open fun getExtension(): String? {
        val possiblyExtension = url.lastIndexOf(".") + 1
        return if (possiblyExtension > 1) {
            return url.toString().substring(possiblyExtension)
        } else {
            val mimeType = http.contentType ?: null
            contentTypeToExtension()[mimeType]
        }
    }

    open fun contentTypeToExtension(): Map<String, String> {
        return mapOf(
            "image/png" to "png",
            "image/jpeg" to "jpg",
            "image/webp" to "webp",
            "image/bmp" to "bmp",
            "image/tiff" to "tiff"
        )
    }


    class BadAddressException : java.lang.Exception {
        constructor() : super() {}
        constructor(message: String?) : super(message) {}
        constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    }

    class UnsupportedFormatException : Exception {
        constructor() : super() {}
        constructor(message: String?) : super(message) {}
        constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    }

    class InvalidFileException : Exception {
        constructor() : super() {}
        constructor(message: String?) : super(message) {}
        constructor(message: String?, cause: Throwable?) : super(message, cause) {}
    }
}