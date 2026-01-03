package no.iktdev.mediaprocessing

import kotlinx.coroutines.delay
import no.iktdev.mediaprocessing.shared.common.DownloadClient
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

class MockDownloadClient(
    private val delayMillis: Long = 0,
    private val throwException: Boolean = false,
    private val mockFile: File? = null
) : DownloadClient(
    outDir = File("/null"),
    connectionFactory = object : ConnectionFactory {
        override fun open(uri: URI): HttpURLConnection {
            throw UnsupportedOperationException("MockDownloadClient does not open real connections")
        }
    }
) {

    override suspend fun download(useUrl: String, useBaseName: String): DownloadResult {
        if (delayMillis > 0) delay(delayMillis)
        if (throwException) throw RuntimeException("Simulated download failure")

        return if (mockFile != null) {
            DownloadResult(success = true, result = mockFile, error = null)
        } else {
            DownloadResult(success = false, result = null, error = "No mock file configured")
        }
    }
}
