package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.TestBase
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystemUse
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CollectionProjectionTest: TestBase() {

    private fun tempOutbox(folders: List<String>): IFile {
        val root = IFile("/tmp").using("collectionProjectionTest")
        root.mkdirs()
        folders.forEach { name -> root.using(name).mkdirs() }
        return root
    }

    @DisplayName(
        """
        Hvis parsedCollection mangler
        Når getCollection kalles
        Så:
            skal det kastes NoSuchElementException
        """
    )
    @Test
    fun noParsedCollection_throws() {
        val start = defaultStartEvent()
            .addToHistory()
        val proj = CollectionProjection(history, tempOutbox(emptyList()))
        assertThrows(NoSuchElementException::class.java) {
            proj.getCollection()
        }
    }

    @DisplayName(
        """
        Hvis parsedCollection finnes
        Og ingen mapper finnes i outbox
        Når getCollection kalles
        Så:
            skal parsedCollection brukes (cleanForFileSystemUse)
        """
    )
    @Test
    fun parsedCollectionUsedWhenNoFolders() {
        val start = defaultStartEvent()
            .addToHistory()
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "My Show",
                parsedFileName = "file",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        ).derivedOf(start)
            .addToHistory()

        val proj = CollectionProjection(history, tempOutbox(emptyList()))
        assertEquals("My Show".cleanForFileSystemUse(), proj.getCollection())
    }

    @DisplayName(
        """
        Hvis metadata-titler finnes
        Og en tittel matcher en eksisterende mappe (etter normalisering)
        Når getCollection kalles
        Så:
            skal matchende mappe returneres
        """
    )
    @Test
    fun metadataMatchesExistingFolder() {
        val start = defaultStartEvent()
            .addToHistory()
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "Fallback",
                parsedFileName = "file",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        ).derivedOf(start)
            .addToHistory()

        val metadata = MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("Main"),
                similarity = 100,
                prefix = 0,
                keywordScore = 0.0,
                typeScore = 0.0,
                completenessScore = 0.0,
                sourceScore = 0.0,
                totalScore = 0.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "x",
                    title = "MatchMe",
                    alternateTitles = listOf("Alt1"),
                    cover = null,
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        )
            .addToHistory()

        val proj = CollectionProjection(
            history,
            tempOutbox(listOf("MatchMe", "Other"))
        )

        assertEquals("MatchMe", proj.getCollection())
    }

    @DisplayName(
        """
        Hvis metadata finnes
        Men ingen metadata-titler matcher eksisterende mapper
        Når getCollection kalles
        Så:
            skal parsedCollection brukes (fallback)
        """
    )
    @Test
    fun fallbackWhenNoMetadataMatch() {
        val start = defaultStartEvent()
            .addToHistory()
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "Fallback",
                parsedFileName = "file",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        ).derivedOf(start)
            .addToHistory()

        val metadata = MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("Main"),
                similarity = 100,
                prefix = 0,
                keywordScore = 0.0,
                typeScore = 0.0,
                completenessScore = 0.0,
                sourceScore = 0.0,
                totalScore = 0.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "x",
                    title = "Title",
                    alternateTitles = listOf("Alt1"),
                    cover = null,
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        )
            .derivedOf(parsed)
            .addToHistory()

        val proj = CollectionProjection(history,
            tempOutbox(listOf("A", "B"))
        )

        assertEquals("Fallback".cleanForFileSystemUse(), proj.getCollection())
    }

    @DisplayName(
        """
        Hvis parsedCollection inneholder unicode
        Og en eksisterende mappe matcher etter cleanForFileSystemUse
        Når getCollection kalles
        Så:
            skal unicode-mappen brukes
        """
    )
    @Test
    fun unicodeMatching() {
        val start = defaultStartEvent()
            .addToHistory()
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "ÆØÅ Show",
                parsedFileName = "file",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        ).derivedOf(start)
            .addToHistory()

        val cleaned = "ÆØÅ Show".cleanForFileSystemUse()

        val proj = CollectionProjection(
            history,
            tempOutbox(listOf(cleaned))
        )

        assertEquals(cleaned, proj.getCollection())
    }

    @DisplayName(
        """
        Hvis metadata-tittel kun matcher ved forskjellig casing
        Når getCollection kalles
        Så:
            skal parsedCollection brukes (systemet er case-sensitive)
        """
    )
    @Test
    fun caseSensitiveMismatch() {
        val start = defaultStartEvent()
            .addToHistory()
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "Fallback",
                parsedFileName = "file",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        ).derivedOf(start)
            .addToHistory()

        val metadata = MetadataSearchResultEvent(
            results = emptyList(),
            recommended = MetadataSearchResultEvent.SearchResult(
                searchTitles = listOf("Main"),
                similarity = 100,
                prefix = 0,
                keywordScore = 0.0,
                typeScore = 0.0,
                completenessScore = 0.0,
                sourceScore = 0.0,
                totalScore = 0.0,
                metadata = MetadataSearchResultEvent.SearchResult.MetadataResult(
                    source = "x",
                    title = "matchme",
                    alternateTitles = emptyList(),
                    cover = null,
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        ).derivedOf(parsed)
            .addToHistory()

        val proj = CollectionProjection(
            history,
            tempOutbox(listOf("MatchMe"))
        )

        assertEquals("MatchMe".cleanForFileSystemUse(), proj.getCollection())
    }
}
