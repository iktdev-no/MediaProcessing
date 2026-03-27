package no.iktdev.mediaprocessing.shared.common

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.iktdev.files.IFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URI
import kotlin.io.path.createTempDirectory
import kotlin.test.assertFailsWith

class DownloadClientTest {

    private fun tempDir(): IFile = createTempDirectory().toFile().absolutePath.let { IFile(it) }

    private fun fakeConnection(data: ByteArray): HttpURLConnection {
        val mock = mockk<HttpURLConnection>()
        every { mock.inputStream } returns data.inputStream()
        return mock
    }

    private fun fakeMetadata(ext: String = "jpg") = DownloadClient.DownloadMetadata(
        uri = URI("http://example.com/file.$ext"),
        mimeType = "image/$ext",
        length = 10
    )

    private fun client(outDir: IFile) = object : DownloadClient(
        outDir = outDir,
        connectionFactory = mockk()
    ) {}

    // ------------------------------------------------------------
    // 1. downloadFile
    // ------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når downloadFile kjøres
        Hvis input stream inneholder bytes
        Så:
            skrives filen til disk og returneres
        """
    )
    fun downloadFile_writes_file() = runTest {
        val outDir = tempDir()
        val data = "hello".toByteArray()

        val connection = fakeConnection(data)
        val client = client(outDir)

        val file = client.downloadFile(connection)

        assertNotNull(file)
        assertTrue(file!!.exists())
        assertEquals("hello", file.readText())
    }

    // ------------------------------------------------------------
    // 2. finalizeDownload (happy path)
    // ------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når finalizeDownload kjøres
        Hvis atomic move lykkes
        Så:
            flyttes temp-filen til endelig filnavn og temp-filen slettes
        """
    )
    fun finalizeDownload_moves_file_atomically() = runTest {
        val outDir = tempDir()
        val tempFile =  outDir.using("temp.downloading").apply { writeText("hello") }

        val client = client(outDir)
        val metadata = fakeMetadata("jpg")

        val result = client.finalizeDownload(tempFile, "final", metadata)

        assertTrue(result.exists())
        assertEquals("final.jpg", result.name)
        assertFalse(tempFile.exists())
    }

    // ------------------------------------------------------------
    // 3. finalizeDownload (move failure)
    // ------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når finalizeDownload kjøres
        Hvis atomic move feiler
        Så:
            kastes InvalidFileException
        """
    )
    fun finalizeDownload_throws_on_failure() = runTest {
        val outDir = tempDir()
        val tempFile = outDir.using("temp.downloading").apply { writeText("hello") }

        // Gjør katalogen skrivebeskyttet for å tvinge move-feil
        outDir.setWritable(false)

        val client = client(outDir)
        val metadata = fakeMetadata("jpg")

        assertFailsWith<DownloadClient.InvalidFileException> {
            client.finalizeDownload(tempFile, "final", metadata)
        }
    }

    // ------------------------------------------------------------
    // 5. getExtension
    // ------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når getExtension kjøres
        Hvis metadata inneholder MIME-type
        Så:
            returneres riktig filendelse
        """
    )
    fun getExtension_from_metadata() = runTest {
        val outDir = tempDir()
        val client = client(outDir)

        val ext = client.getExtension(IFile("x"), fakeMetadata("png"))

        assertEquals("png", ext)
    }

    @Test
    @DisplayName(
        """
    Når getExtension kjøres
    Hvis metadata mangler MIME-type
    Så:
        returneres null
    """
    )
    fun getExtension_returns_null_without_mime() = runTest {
        val outDir = tempDir()
        val client = client(outDir)

        val tempFile = outDir.using("dummy").apply { writeText("irrelevant") }

        val metadata = DownloadClient.DownloadMetadata(
            uri = URI("http://example.com/file"),
            mimeType = null,
            length = 10
        )

        val ext = client.getExtension(tempFile, metadata)

        assertNull(ext)
    }

}
