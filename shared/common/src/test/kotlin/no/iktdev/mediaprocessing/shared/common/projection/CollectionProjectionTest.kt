package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.TestBase
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystemUse
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CollectionProjectionTest : TestBase() {

    @DisplayName(
        """
        Hvis verken parsedCollection eller metadata finnes
        Når getCollection kalles
        Så:
            skal det kastes NoSuchElementException
        """
    )
    @Test
    fun noCandidates_throws() {
        defaultStartEvent().addToHistory()
        val proj = CollectionProjection(history)
        assertThrows(NoSuchElementException::class.java) {
            proj.getCollection()
        }
    }

    @DisplayName(
        """
        Hvis kun parsedCollection finnes
        Når getCollection kalles
        Så:
            skal parsedCollection brukes (cleanForFileSystemUse)
        """
    )
    @Test
    fun parsedCollectionUsedAsFallback() {
        val start = defaultStartEvent().addToHistory()
        MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "My Show",
                parsedFileName = "file",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        ).derivedOf(start).addToHistory()

        val proj = CollectionProjection(history)
        assertEquals("My Show".cleanForFileSystemUse(), proj.getCollection())
    }

    @DisplayName(
        """
        Hvis metadata har tittel
        Når getCollection kalles
        Så:
            skal metadata-tittel prioriteres foran parsedCollection
        """
    )
    @Test
    fun metadataTitlePrioritizedOverParsed() {
        val start = defaultStartEvent().addToHistory()
        val parsed = MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "Fallback Show",
                parsedFileName = "file",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        ).derivedOf(start).addToHistory()

        MetadataSearchResultEvent(
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
                    title = "Metadata Title",
                    alternateTitles = listOf("Alt1"),
                    cover = null,
                    bannerImage = null,
                    type = MediaType.Movie,
                    summary = emptyList(),
                    genres = emptyList()
                )
            ),
            status = TaskStatus.Completed
        ).derivedOf(parsed).addToHistory()

        val proj = CollectionProjection(history)
        assertEquals("Metadata Title", proj.getCollection())
    }

    @DisplayName(
        """
        Hvis DeterminedCollectionTaskResultEvent finnes i historikken
        Når getCollection kalles
        Så:
            skal den lagrede kolleksjonen returneres direkte
        """
    )
    @Test
    fun usesAlreadyDeterminedCollectionEvent() {
        val start = defaultStartEvent().addToHistory()
        DeterminedCollectionTaskResultEvent(
            status = TaskStatus.Completed,
            collection = "LockedInCollection"
        ).derivedOf(start).addToHistory()

        val proj = CollectionProjection(history)
        assertEquals("LockedInCollection", proj.getCollection())
    }

    @DisplayName(
        """
        Hvis parsedCollection inneholder unicode
        Når getCollection kalles
        Så:
            skal cleanForFileSystemUse kjøres på den
        """
    )
    @Test
    fun unicodeHandling() {
        val start = defaultStartEvent().addToHistory()
        MediaParsedInfoEvent(
            data = MediaParsedInfoEvent.ParsedData(
                parsedCollection = "ÆØÅ Show",
                parsedFileName = "file",
                parsedSearchTitles = emptyList(),
                mediaType = MediaType.Movie
            )
        ).derivedOf(start).addToHistory()

        val proj = CollectionProjection(history)
        assertEquals("ÆØÅ Show".cleanForFileSystemUse(), proj.getCollection())
    }

    @DisplayName(
        """
        Hvis operasjonen er ConvertSubtitles (alternativ flyt)
        Når getCollection kalles
        Så:
            skal kolleksjonen hentes fra fil-URI-strukturen
        """
    )
    @Test
    fun altFlowConvertSubtitles() {
        val start = StartProcessingEvent(
            StartData(
                fileUri = IFile("/storage/collectionName/sub/lang/file.srt").absolutePath,
                operation = setOf(OperationType.ConvertSubtitles)
            )
        ).addToHistory()

        val proj = CollectionProjection(history)
        assertEquals("collectionName", proj.getCollection())
    }
}