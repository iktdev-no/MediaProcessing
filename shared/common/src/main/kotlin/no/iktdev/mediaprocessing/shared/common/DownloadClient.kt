package no.iktdev.mediaprocessing.shared.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.iktdev.files.IFile
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

open class DownloadClient(val outDir: IFile, private val connectionFactory: ConnectionFactory) {
    val log = KotlinLogging.logger {}
    private val BUFFER_SIZE = 4096

    open fun onCreate() {}

    fun HttpURLConnection.getMetadata(): DownloadMetadata {
        return DownloadMetadata(
            this.url.toURI(),
            this.contentType.also {
                if (it.isNullOrBlank()) {
                    log.error { "Unable to determine mime type for $url" }
                } else {
                    log.info { "Downloading file from $url with mime type $it" }
                }
            },
            this.contentLengthLong
        )
    }

    protected fun getProgress(read: Int, total: Int): Int {
        return if (total == 0) 0 else ((read * 100) / total)
    }

    open suspend fun download(useUrl: String, useBaseName: String): DownloadResult {
        // 1. Sjekk om fil allerede finnes
        val existing = outDir.listFiles { _, name ->
            name.startsWith("$useBaseName.")
        }?.firstOrNull()

        if (existing != null) {
            log.info { "File already exists: ${existing.absolutePath}, skipping download" }
            return DownloadResult(true, existing, null)
        }


        return try {
            val connection = connectionFactory.open(URI(useUrl))
            val metadata = connection.getMetadata()
            val downloadedFile = downloadFile(connection)
            val resultFile = downloadedFile?.let { file ->
                finalizeDownload(file, useBaseName, metadata)
            }
            DownloadResult(resultFile?.exists() == true, resultFile, null)
        } catch (e: Exception) {
            DownloadResult(false, null, e.message)
        }
    }

    open suspend fun downloadFile(useConnection: HttpURLConnection) = withContext(Dispatchers.IO) {
        val downloadFile: IFile = outDir.using(UUID.randomUUID().toString() + ".downloading")
        if (downloadFile.exists()) {
            log.info { "${downloadFile.name} already exists. Download skipped!" }
            return@withContext null
        }

        var totalBytesRead = 0
        val buffer = ByteArray(BUFFER_SIZE)
        useConnection.inputStream.use { input ->
            downloadFile.toJavaFile().outputStream().use { output ->
                var bytesRead = input.read(buffer)
                while (bytesRead >= 0) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    bytesRead = input.read(buffer)
                    // System.out.println(getProgress(totalBytesRead))
                }
            }
        }

        downloadFile
    }

    open suspend fun finalizeDownload(tempFile: IFile, baseName: String, metadata: DownloadMetadata): IFile = withContext(
        Dispatchers.IO) {
        val extension = getExtension(tempFile, metadata)
            ?: throw UnsupportedFormatException("Downloaded file does not contain a supported file extension")

        val outFile = outDir.using("$baseName.$extension")

        try {
            Files.move(
                tempFile.toPath(),
                outFile.toPath(),
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: Exception) {
            log.error { "Failed to atomically move ${tempFile.name} to ${outFile.name}" }
            throw InvalidFileException("Failed to finalize downloaded file")
        }

        outFile
    }



    open fun getExtension(outFile: IFile, metadata: DownloadMetadata): String? {
        return mimeToExtension(metadata.mimeType)
            ?: outFile.getFileType()
    }


    fun mimeToExtension(mimeType: String?): String? {
        return when (mimeType) {
            "image/png" -> "png"
            "image/jpg", "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "image/bmp" -> "bmp"
            "image/tiff" -> "tiff"
            else -> null
        }
    }

    fun IFile.getFileType(): String? {
        val bytes = this.openInputStream().use { it.readNBytes(12) } // les første 12 bytes
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

    data class DownloadMetadata(
        val uri: URI,
        val mimeType: String?,
        val length: Long
    )

    data class DownloadResult(
        val success: Boolean,
        val result: IFile? = null,
        val error: String? = null
    )

    interface ConnectionFactory {
        fun open(uri: URI): HttpURLConnection
    }

    class DefaultConnectionFactory : ConnectionFactory {
        override fun open(uri: URI): HttpURLConnection {
            try {
                return uri.toURL().openConnection() as HttpURLConnection
            } catch (e: Exception) {
                e.printStackTrace()
                throw BadAddressException("Provided url is either not provided (null) or is not a valid http url")
            }
        }

    }
}