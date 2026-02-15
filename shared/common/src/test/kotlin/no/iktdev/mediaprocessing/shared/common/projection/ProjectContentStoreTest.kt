package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystem
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

    class MockMigrateContentProject(events: List<Event>, val folders: List<String>?) :
        MigrateContentProject(events, Files.newTemporaryFolder()) {
        override fun getFoldersInStore(): List<String> {
            return folders ?: emptyList()
        }
    }

    private fun store(events: List<Event>, folders: List<String>) = MockMigrateContentProject(events, folders)

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
        val store = MigrateContentProject(events, temp)

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
        val store = MigrateContentProject(events, temp)

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
        val store = MigrateContentProject(events, temp)

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


    // ---------------------------------------------------------
    // getDesiredCollection()
    // ---------------------------------------------------------

    @DisplayName(
        """
        Hvis ingen MediaParsedInfoEvent finnes
        Når getDesiredCollection kalles
        Så:
            returneres null
    """
    )
    @Test
    fun desiredCollection_none() {
        // Arrange
        val store = store(events = emptyList(), folders = emptyList())

        // Act
        val result = store.getDesiredCollection()

        // Assert
        assertNull(result)
    }

    @DisplayName(
        """
        Hvis MediaParsedInfoEvent finnes
        Når getDesiredCollection kalles
        Så:
            returneres parsedCollection
    """
    )
    @Test
    fun desiredCollection_found() {
        // Arrange
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyCollection",
                parsedFileName = "file.mkv",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        )

        val store = store(events = listOf(parsed), folders = emptyList())

        // Act
        val result = store.getDesiredCollection()

        // Assert
        assertEquals("MyCollection", result)
    }

    // ---------------------------------------------------------
    // getMetadataTitles()
    // ---------------------------------------------------------

    @DisplayName(
        """
        Hvis ingen MetadataSearchResultEvent finnes
        Når getMetadataTitles kalles
        Så:
            returneres tom liste
    """
    )
    @Test
    fun metadataTitles_none() {
        // Arrange
        val store = store(events = emptyList(), folders = emptyList())

        // Act
        val result = store.getMetadataTitles()

        // Assert
        assertTrue(result.isEmpty())
    }

    @DisplayName(
        """
        Hvis MetadataSearchResultEvent finnes med recommended
        Når getMetadataTitles kalles
        Så:
            returneres alternateTitles + title
    """
    )
    @Test
    fun metadataTitles_found() {
        // Arrange
        val metadata = MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("MainTitle"),
                similarity = 100,
                prefix = 10,
                keywordScore = 20.0,
                typeScore = 80.0,
                completenessScore = 15.0,
                sourceScore = 5.0,
                totalScore = 230.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "test",
                    title = "MainTitle",
                    alternateTitles = listOf("Alt1", "Alt2"),
                    cover = "cover.jpg",
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        )


        val store = store(events = listOf(metadata), folders = emptyList())

        // Act
        val result = store.getMetadataTitles()

        // Assert
        assertEquals(listOf("Alt1", "Alt2", "MainTitle"), result)
    }

    // ---------------------------------------------------------
    // getDesiredStoreFolder()
    // ---------------------------------------------------------

    @DisplayName(
        """
        Hvis parsedCollection mangler
        Når getDesiredStoreFolder kalles
        Så:
            returneres null
    """
    )
    @Test
    fun desiredStore_noCollection() {
        // Arrange
        val store = store(events = emptyList(), folders = emptyList())

        // Act
        val result = store.getDesiredStoreFolder()

        // Assert
        assertNull(result)
    }

    @DisplayName(
        """
        Hvis parsedCollection finnes
        Og ingen mapper finnes i store
        Når getDesiredStoreFolder kalles
        Så:
            returneres assuredStore (<storage>/<collection>)
    """
    )
    @Test
    fun desiredStore_noFolders() {
        // Arrange
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "MyCollection",
                parsedFileName = "file.mkv",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        )

        val store = store(events = listOf(parsed), folders = emptyList())

        // Act
        val result = store.getDesiredStoreFolder()

        // Assert
        assertNotNull(result)
        assertEquals("MyCollection", result!!.name)
    }

    @DisplayName(
        """
        Hvis parsedCollection finnes
        Og mapper finnes i store
        Og metadata-titler matcher en eksisterende mappe
        Når getDesiredStoreFolder kalles
        Så:
            returneres mappen som matcher metadata
    """
    )
    @Test
    fun desiredStore_matchMetadata() {
        // Arrange
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "FallbackCollection",
                parsedFileName = "file.mkv",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        )

        val metadata = MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("MainTitle"),
                similarity = 100,
                prefix = 10,
                keywordScore = 20.0,
                typeScore = 80.0,
                completenessScore = 15.0,
                sourceScore = 5.0,
                totalScore = 230.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "test",
                    title = "MatchMe",
                    alternateTitles = listOf("Alt1"),
                    cover = "cover.jpg",
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        )


        val store = store(
            events = listOf(parsed, metadata),
            folders = listOf("MatchMe", "Other")
        )

        // Act
        val result = store.getDesiredStoreFolder()

        // Assert
        assertNotNull(result)
        assertEquals("MatchMe", result!!.name)
    }

    @DisplayName(
        """
        Hvis parsedCollection finnes
        Og mapper finnes i store
        Og ingen metadata-titler matcher
        Når getDesiredStoreFolder kalles
        Så:
            returneres assuredStore (parsedCollection)
    """
    )
    @Test
    fun desiredStore_noMatch() {
        // Arrange
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "FallbackCollection",
                parsedFileName = "file.mkv",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        )

        val metadata = MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("MainTitle"),
                similarity = 100,
                prefix = 10,
                keywordScore = 20.0,
                typeScore = 80.0,
                completenessScore = 15.0,
                sourceScore = 5.0,
                totalScore = 230.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "test",
                    title = "MainTitle",
                    alternateTitles = listOf("Alt1", "Alt2"),
                    cover = "cover.jpg",
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        )


        val store = store(
            events = listOf(parsed, metadata),
            folders = listOf("FolderA", "FolderB")
        )

        // Act
        val result = store.getDesiredStoreFolder()

        // Assert
        assertNotNull(result)
        assertEquals("FallbackCollection", result!!.name)
    }


    data class DesiredStoreCase(
        val name: String,
        val parsedCollection: String?,
        val metadataTitles: List<String>,
        val existingFolders: List<String>,
        val expectedFolder: String?
    )

    @DisplayName("""
    Hvis parsedCollection varierer
    Når getDesiredStoreFolder kalles
    Så:
        skal resultatet følge parsedCollection-reglene
    """)
    @ParameterizedTest()
    @MethodSource("desiredStoreCases")
    fun desiredStore_parsedCollectionLogic(case: DesiredStoreCase) {
        if (case.parsedCollection == null) {
            val store = MockMigrateContentProject(events = emptyList(), folders = case.existingFolders)
            assertNull(store.getDesiredStoreFolder())
            return
        }
    }

    @DisplayName("""
    Hvis metadata-titler finnes
    Når getDesiredStoreFolder kalles
    Så:
        skal riktig matchende mappe velges
    """)
    @ParameterizedTest()
    @MethodSource("desiredStoreCases")
    fun desiredStore_metadataMatching(case: DesiredStoreCase) {
        if (case.metadataTitles.isEmpty()) return
        if (case.parsedCollection == null) return

        val events = mutableListOf<Event>()

        events += MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = case.parsedCollection,
                parsedFileName = "file.mkv",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        )

        events += MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("MainTitle"),
                similarity = 100,
                prefix = 10,
                keywordScore = 20.0,
                typeScore = 80.0,
                completenessScore = 15.0,
                sourceScore = 5.0,
                totalScore = 230.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "test",
                    title = case.metadataTitles.last(),
                    alternateTitles = case.metadataTitles.dropLast(1),
                    cover = "cover.jpg",
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        )


        val store = MockMigrateContentProject(events, case.existingFolders)

        val result = store.getDesiredStoreFolder()

        assertEquals(case.expectedFolder, result?.name)
    }

    @DisplayName("""
    Hvis metadata finnes men ingen mapper matcher
    Når getDesiredStoreFolder kalles
    Så:
        skal fallback (parsedCollection) brukes
""")
    @ParameterizedTest()
    @MethodSource("desiredStoreCases")
    fun desiredStore_fallbackLogic(case: DesiredStoreCase) {
        if (case.parsedCollection == null) return
        if (case.metadataTitles.isEmpty()) return
        if (case.expectedFolder == case.metadataTitles.firstOrNull()) return

        val events = mutableListOf<Event>()

        events += MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = case.parsedCollection,
                parsedFileName = "file.mkv",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        )

        events += MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("MainTitle"),
                similarity = 100,
                prefix = 10,
                keywordScore = 20.0,
                typeScore = 80.0,
                completenessScore = 15.0,
                sourceScore = 5.0,
                totalScore = 230.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "test",
                    title = case.metadataTitles.last(),
                    alternateTitles = case.metadataTitles.dropLast(1),
                    cover = "cover.jpg",
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        )


        val store = MockMigrateContentProject(events, case.existingFolders)

        val result = store.getDesiredStoreFolder()

        assertEquals(case.expectedFolder, result?.name)
    }


    companion object {

        @JvmStatic
        fun desiredStoreCases() = listOf(
            DesiredStoreCase(
                name = "No parsed collection → null",
                parsedCollection = null,
                metadataTitles = emptyList(),
                existingFolders = emptyList(),
                expectedFolder = null
            ),
            DesiredStoreCase(
                name = "Parsed collection, no folders → assuredStore",
                parsedCollection = "MyShow",
                metadataTitles = emptyList(),
                existingFolders = emptyList(),
                expectedFolder = "MyShow"
            ),
            DesiredStoreCase(
                name = "Metadata matches existing folder",
                parsedCollection = "Fallback",
                metadataTitles = listOf("MatchMe"),
                existingFolders = listOf("MatchMe", "Other"),
                expectedFolder = "MatchMe"
            ),
            DesiredStoreCase(
                name = "Metadata alternate title matches",
                parsedCollection = "Fallback",
                metadataTitles = listOf("Alt1", "MainTitle"),
                existingFolders = listOf("Alt1"),
                expectedFolder = "Alt1"
            ),
            DesiredStoreCase(
                name = "Multiple metadata titles, first match wins",
                parsedCollection = "Fallback",
                metadataTitles = listOf("Nope", "YesMatch", "Another"),
                existingFolders = listOf("YesMatch", "Another"),
                expectedFolder = "YesMatch"
            ),
            DesiredStoreCase(
                name = "Metadata titles exist but none match → fallback",
                parsedCollection = "Fallback",
                metadataTitles = listOf("A", "B", "C"),
                existingFolders = listOf("X", "Y", "Z"),
                expectedFolder = "Fallback"
            ),
            DesiredStoreCase(
                name = "Weird folder names (spaces, unicode)",
                parsedCollection = "Fallback",
                metadataTitles = listOf("ÆØÅ Show"),
                existingFolders = listOf("ÆØÅ Show".cleanForFileSystem()),
                expectedFolder = "AEOA Show"
            ),
            DesiredStoreCase(
                name = "Case-insensitive mismatch → fallback",
                parsedCollection = "Fallback",
                metadataTitles = listOf("matchme"),
                existingFolders = listOf("MatchMe"),
                expectedFolder = "Fallback" // system is case-sensitive
            ),
            DesiredStoreCase(
                name = "Existing folders but metadata empty → fallback",
                parsedCollection = "Fallback",
                metadataTitles = emptyList(),
                existingFolders = listOf("A", "B"),
                expectedFolder = "Fallback"
            )
        )
    }



}