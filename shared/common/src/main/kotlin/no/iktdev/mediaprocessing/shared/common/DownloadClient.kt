package no.iktdev.mediaprocessing.shared.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.iktdev.exfl.using
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.UUID
import kotlin.apply
import kotlin.io.use
import kotlin.run
import kotlin.text.lastIndexOf
import kotlin.text.substring
import kotlin.to

open class DownloadClient(val url: String, val outDir: File, val baseName: String) {
    val log = KotlinLogging.logger {}
    protected val http: HttpURLConnection = openConnection()
    private val BUFFER_SIZE = 4096

    private fun openConnection(): HttpURLConnection {
        try {
            return URI(url).toURL().openConnection() as HttpURLConnection
        } catch (e: Exception) {
            e.printStackTrace()
            throw BadAddressException("Provided url is either not provided (null) or is not a valid http url")
        }
    }

    protected fun getLength(): Int = http.contentLength


    protected fun getProgress(read: Int, total: Int = getLength()): Int {
        return ((read * 100) / total)
    }

    suspend fun download(): File? = withContext(Dispatchers.IO) {
        val downloadFile = outDir.using(UUID.randomUUID().toString() + ".downloading")

        if (downloadFile.exists()) {
            log.info { "${downloadFile.name} already exists. Download skipped!" }
            return@withContext null
        }

        val inputStream = http.inputStream
        val mimeType: String? = http.contentType
        if (mimeType == null) {
            log.error { "Unable to determine mime type for $url" }
        } else {
            log.info { "Downloading file from $url with mime type $mimeType" }
        }

        val fos = FileOutputStream(downloadFile, false)

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

        val extension = getExtension(downloadFile, mimeType ?: "")
            ?: throw UnsupportedFormatException("Downloaded file does not contain a supported file extension")

        val outFile = outDir.using("$baseName.$extension")
        val renamed = downloadFile.renameTo(outFile)
        if (!renamed) {
            log.error { "Failed to rename ${downloadFile.name} to ${outFile.name}" }
            throw InvalidFileException("Failed to rename downloaded file")
        }

        return@withContext outFile
    }

    open fun getExtension(outFile: File, mimeType: String): String? {
        val extensionFormat = mimeToExtension(mimeType) ?: outFile.getFileType()
        if (extensionFormat == null) {
            val possiblyExtension =  url.lastIndexOf(".") + 1
            if (possiblyExtension > 1) {
                return url.substring(possiblyExtension)
            }
        }
        return null
    }

    fun mimeToExtension(mimeType: String): String? {
        return when(mimeType) {
            "image/png" -> "png"
            "image/jpg", "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "image/bmp" -> "bmp"
            "image/tiff" -> "tiff"
            else -> null
        }
    }

    fun File.getFileType(): String? {
        val bytes = this.inputStream().use { it.readNBytes(12) } // les første 12 bytes
        return when {
            // JPEG: FF D8 FF
            bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "jpg"

            // PNG: 89 50 4E 47
            bytes.size >= 4 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                    bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "png"

            // GIF: "GIF8"
            bytes.size >= 4 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                    bytes[2] == 'F'.code.toByte() && bytes[3] == '8'.code.toByte() -> "gif"

            // WEBP: RIFF....WEBP
            bytes.size >= 12 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
                    bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
                    bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
                    bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte() -> "webp"

            // BMP: 42 4D ("BM")
            bytes.size >= 2 && bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> "bmp"

            // TIFF: enten "II*" eller "MM*"
            bytes.size >= 4 && (
                    (bytes[0] == 'I'.code.toByte() && bytes[1] == 'I'.code.toByte() && bytes[2] == 0x2A.toByte()) ||
                            (bytes[0] == 'M'.code.toByte() && bytes[1] == 'M'.code.toByte() && bytes[2] == 0x2A.toByte())
                    ) -> "tiff"

            else -> null
        }
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