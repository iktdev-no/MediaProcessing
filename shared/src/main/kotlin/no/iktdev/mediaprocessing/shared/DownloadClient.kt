package no.iktdev.mediaprocessing.shared

import no.iktdev.exfl.using
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

open class DownloadClient(val url: String, val outDir: File, val baseName: String) {
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

    suspend fun download(): File? {
        val extension = getExtension()
            ?: throw UnsupportedFormatException("Provided url does not contain a supported file extension")
        val outFile = outDir.using("$baseName.$extension")
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
        return outFile
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