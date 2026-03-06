package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystemUse
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.assertj.core.util.Files
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class ProjectContentStoreTest {

    class MockMigrateContentProject(collection: String, events: List<Event>, val folders: List<String>?) :
        MigrateContentProject(collection, events, Files.newTemporaryFolder()) {
    }

    private fun store(collection: String, events: List<Event>, folders: List<String>) = MockMigrateContentProject(collection, events, folders)

    fun getTempFolder(): File {
        return File("/tmp")
    }


    @DisplayName(
        """
    Hvis encode-resultatet inneholder cachedOutputFile
    Når getVideoStoreFile kalles
    Så:
        skal video lagres under <storage>/<collection>/<videoFilNavn>
    """
    )
    @Test
    fun videoPath_isCorrect() {
        // Arrange
        val temp = getTempFolder().using("store")

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyShow",
                parsedFileName = "episode1",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Serie
            )
        )

        val encode = ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = "/tmp/cache/videoEncoded.mp4"
            ),
            status = TaskStatus.Completed
        )

        val events = listOf(parsed, encode)
        val store = MigrateContentProject("MyShow", events, temp)

        // Act
        val result = store.getVideoStoreFile()

        // Assert
        assertNotNull(result)
        assertEquals("episode1.mp4", result!!.storeFile.name)
        assertEquals("MyShow", result.storeFile.parentFile.name)
        assertEquals(temp, result.storeFile.parentFile.parentFile)
    }

    @DisplayName(
        """
    Hvis extract- og convert-events inneholder undertekstfiler
    Når getSubtitleStoreFiles kalles
    Så:
        skal filer lagres under <storage>/<collection>/sub/<language>/<filnavn>
    """
    )
    @Test
    fun subtitlePaths_areCorrect() {
        // Arrange
        val temp = getTempFolder().using("store")

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyShow",
                parsedFileName = "episode1",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Serie
            )
        )

        val extract = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                language = "eng",
                cachedOutputFile = "/tmp/cache/sub1.srt"
            )
        )

        val convert = ConvertTaskResultEvent(
            data = ConvertTaskResultEvent.ConvertedData(
                language = "eng",
                baseName = "sub1",
                outputFiles = listOf("/tmp/cache/sub1.vtt")
            ),
            status = TaskStatus.Completed
        )

        val events = listOf(parsed, extract, convert)
        val store = MigrateContentProject("MyShow", events, temp)

        // Act
        val results = store.getSubtitleStoreFiles()

        // Assert
        assertEquals(2, results?.size)

        results?.forEach { entry ->
            val file = entry.cts.storeFile

            // Filnavn
            assertTrue(file.name == "episode1.srt" || file.name == "episode1.vtt")

            // <language>
            assertEquals("eng", file.parentFile.name)

            // sub/
            assertEquals("sub", file.parentFile.parentFile.name)

            // <collection>
            assertEquals("MyShow", file.parentFile.parentFile.parentFile.name)

            // <storage>
            assertEquals(temp, file.parentFile.parentFile.parentFile.parentFile)
        }
    }


    @DisplayName(
        """
    Hvis cover-download-event inneholder en coverfil
    Når getCoverStoreFiles kalles
    Så:
        skal cover lagres under <storage>/<collection>/<coverFilNavn>
    """
    )
    @Test
    fun coverPath_isCorrect() {
        // Arrange
        val temp = getTempFolder().using("store")

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyShow",
                parsedFileName = "episode1",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Serie
            )
        )

        val cover = CoverDownloadResultEvent(
            data = CoverDownloadResultEvent.CoverDownloadedData(
                source = "test",
                outputFile = "/tmp/cache/cover.jpg"
            ),
            status = TaskStatus.Completed
        )

        val events = listOf(parsed, cover)
        val store = MigrateContentProject("MyShow",events, temp)

        // Act
        val results = store.getCoverStoreFiles()

        // Assert
        assertEquals(1, results?.size)
        val entry = results?.first()
        assertNotNull(entry)

        assertEquals("MyShow.jpg", entry!!.storeFile.name)
        assertEquals("MyShow", entry!!.storeFile.parentFile.name)
        assertEquals(temp, entry!!.storeFile.parentFile.parentFile)
    }


}