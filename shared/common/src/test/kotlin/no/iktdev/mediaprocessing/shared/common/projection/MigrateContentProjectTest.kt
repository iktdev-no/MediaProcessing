package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystemUse
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent.ParsedData
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class MigrateContentProjectPathTest {

    private fun tempDir(): File {
        val dir = createTempDir(prefix = "store-test-")
        dir.deleteOnExit()
        return dir
    }

    @Test
    @DisplayName(
        """
        Når projeksjonen beregner video-path
        Så:
            Skal pathen alltid ligge under storageArea
        """
    )
    fun videoPathIsUnderStorageArea() {
        val storage = tempDir()

        val events = listOf<Event>(
            MediaParsedInfoEvent(
                ParsedData(
                    parsedCollection = "Breaking Bad",
                    parsedFileName = "bb.s01e01",
                    parsedSearchTitles = emptyList(),
                    mediaType = MediaType.Serie
                )
            ),
            ProcesserEncodeResultEvent(
                status = TaskStatus.Completed,
                data = ProcesserEncodeResultEvent.EncodeResult(
                    cachedOutputFile = "/tmp/encoded.mp4"
                )
            )
        )

        val project = MigrateContentProject("Breaking Bad", events, storage)

        val video = project.getVideoStoreFile()
        assertNotNull(video)

        val storeFile = video!!.storeFile

        assertTrue(
            storeFile.absolutePath.startsWith(storage.absolutePath),
            "Store path must be inside storageArea"
        )

        assertEquals(
            File(storage, "Breaking Bad/bb.s01e01.mp4").absolutePath,
            storeFile.absolutePath
        )
    }

    @Test
    @DisplayName(
        """
        Når metadata-titler matcher eksisterende mapper
        Så:
            Skal projeksjonen bruke eksisterende mappe
        """
    )
    fun usesExistingCollectionFolder() {
        val storage = tempDir()
        val existing = File(storage, "Breaking Bad")
        existing.mkdirs()

        val events = listOf<Event>(
            MediaParsedInfoEvent(
                ParsedData(
                    parsedCollection = "bb",
                    parsedFileName = "bb.s01e01",
                    parsedSearchTitles = emptyList(),
                    mediaType = MediaType.Serie
                )
            ),
            MetadataSearchResultEvent(
                results = emptyList(),
                recommended = MetadataSearchResultEvent.SearchResult(
                    searchTitles = listOf("Foo"),
                    similarity = 85,
                    prefix = 10,
                    keywordScore = 20.0,
                    typeScore = 80.0,
                    completenessScore = 15.0,
                    sourceScore = 5.0,
                    totalScore = 215.0,
                    metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                        source = "tmdb",
                        title = "Breaking Bad",
                        alternateTitles = listOf("BB"),
                        cover = "x.jpg",
                        bannerImage = null,
                        type = MediaType.Serie,
                        summary = emptyList(),
                        genres = emptyList()
                    )
                ),
                status = TaskStatus.Completed
            )

        )

        val project = MigrateContentProject("Breaking Bad", events, storage)

        assertEquals(
            existing.absolutePath,
            project.useStore!!.absolutePath
        )
    }

    @Test
    fun cleanForFileSystem_transliteration() {
        assertEquals("Senor de los Cielos", "Señor de los Cielos".cleanForFileSystemUse())
        assertEquals("Amelie (2001)", "Amélie (2001)".cleanForFileSystemUse())
        assertEquals("Ubermensch", "Übermensch".cleanForFileSystemUse())
        assertEquals("Lodz, Polska", "Łódź, Polska".cleanForFileSystemUse())
    }

    @Test
    fun cleanForFileSystem_removesSpecialCharacters() {
        assertEquals("Hello World!", "Hello@World!".cleanForFileSystemUse())
        assertEquals("Spider-Man No Way Home!", "Spider-Man: No Way Home!".cleanForFileSystemUse())
    }
    @Test
    fun videoStoreFile_usesSanitizedName() {
        val temp = File("build/test-folder/file")

        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "Señor de los Cielos",
                parsedFileName = "Amélie (2001)",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        )

        val encode = ProcesserEncodeResultEvent(
            data = ProcesserEncodeResultEvent.EncodeResult(
                cachedOutputFile = "/tmp/cache/video.mp4"
            ),
            status = TaskStatus.Completed
        )

        val store = MigrateContentProject("Señor de los Cielos", listOf(parsed, encode), temp)
        val result = store.getVideoStoreFile()

        assertNotNull(result)
        assertEquals("Amelie (2001).mp4", result!!.storeFile.name)
        assertEquals("Señor de los Cielos", result.storeFile.parentFile.name)
    }

    @Test
    fun subtitleStoreFiles_useSanitizedNames() {
        val temp = File("build/test-folder/file")


        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "Señor de los Cielos",
                parsedFileName = "Niña Épica",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Serie
            )
        )

        val extract = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                language = "spa",
                cachedOutputFile = "/tmp/cache/sub1.srt"
            )
        )

        val store = MigrateContentProject("Señor de los Cielos", listOf(parsed, extract), temp)
        val results = store.getSubtitleStoreFiles()

        assertNotNull(results)
        val file = results!!.first().cts.storeFile

        assertEquals("Nina Epica.srt", file.name)
        assertEquals("spa", file.parentFile.name)
        assertEquals("sub", file.parentFile.parentFile.name)
        assertEquals("Señor de los Cielos", file.parentFile.parentFile.parentFile.name)
    }

    @Test
    fun coverStoreFiles_useSanitizedNames() {
        val temp = File("build/test-folder/file")


        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "João e Maria",
                parsedFileName = "João e Maria",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        )

        val cover = CoverDownloadResultEvent(
            data = CoverDownloadResultEvent.CoverDownloadedData(
                source = "tmdb",
                outputFile = "/tmp/cache/cover.jpg"
            ),
            status = TaskStatus.Completed
        )

        val store = MigrateContentProject("João e Maria", listOf(parsed, cover), temp)
        val results = store.getCoverStoreFiles()

        assertNotNull(results)
        val file = results!!.first().storeFile

        assertEquals("Joao e Maria.jpg", file.name)
    }



}
