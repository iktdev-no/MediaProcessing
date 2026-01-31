package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent.ParsedData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
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

        val project = MigrateContentProject(events, storage)

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
                    simpleScore = 10,
                    prefixScore = 10,
                    advancedScore = 10,
                    sourceWeight = 1f,
                    metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                        source = "tmdb",
                        title = "Breaking Bad",
                        alternateTitles = listOf("BB"),
                        cover = "x.jpg",
                        type = MediaType.Serie,
                        summary = emptyList(),
                        genres = emptyList()
                    )
                ),
                status = TaskStatus.Completed
            )
        )

        val project = MigrateContentProject(events, storage)

        assertEquals(
            existing.absolutePath,
            project.useStore!!.absolutePath
        )
    }
}
