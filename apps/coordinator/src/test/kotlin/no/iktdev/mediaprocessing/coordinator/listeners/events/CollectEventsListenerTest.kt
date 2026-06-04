package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.MockData.convertEvent
import no.iktdev.mediaprocessing.MockData.coverEvent
import no.iktdev.mediaprocessing.MockData.determineCollectionEvents
import no.iktdev.mediaprocessing.MockData.encodeEvent
import no.iktdev.mediaprocessing.MockData.extractEvent
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.MockData.metadataEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test


class CollectEventsListenerTest : TestBase() {
    private val listener = CollectEventsListener(eventStore)


    @Test
    @DisplayName(
        """
        Hvis historikken har alle påkrevde hendelser og alle oppgaver er i en gyldig tilstand
        Når onEvent kalles og projeksjonen tilsier gyldig status
        Så:
            Opprettes CollectEvent basert på historikken
    """
    )
    fun success1() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(parsed)
            .addToHistory()

        val encode = encodeEvent("/tmp/video.mp4", parsed)
            .addToHistory()
        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
            .addToHistory()
        val convert = convertEvent(language = "en", baseName = "sub1", outputFiles = listOf("/tmp/sub1.vtt"), derivedFrom = extract.last())
            .addToHistory()
        val cover = coverEvent("/tmp/cover.jpg", metadata.last())
            .addToHistory()
        val determined = determineCollectionEvents(collection = "MyCollection", cover.last())
            .addToHistory()

        val result = listener.onEvent(cover.last(), history)

        assertThat(result).isInstanceOf(CollectedEvent::class.java)
    }



    @Test
    @DisplayName(
        """
    Hvis vi har kun encoded hendelse, men vi har sagt at vi også skal ha extract, men ikke har opprettet extract
    Når encode result kommer inn
    Så:
        Opprettes ikke CollectEvent
    """
    )
    fun success2() {
        val started = defaultStartEvent().let { ev ->
            ev.copy(
                data = ev.data.copy(
                    operation = setOf(
                        OperationType.MetadataSearch,
                        OperationType.Encode,
                        OperationType.ExtractSubtitles
                    )
                )
            )
        }.newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(parsed).first()
            .addToHistory()
        val encode = encodeEvent("/tmp/video.mp4", parsed)
            .addToHistory()

        val result = listener.onEvent(encode.last(), history)

        assertThat(result).isNull()
    }


    @Test
    @DisplayName(
        """
        Hvis vi har kun convert hendelse
        Når convert har kommet inn
        Så:
            Opprettes CollectEvent basert på historikken
    """
    )
    fun success3() {
        val started = defaultStartEvent().let { ev ->
            ev.copy(
                data = ev.data.copy(
                    operation = setOf(
                        OperationType.MetadataSearch,
                        OperationType.ConvertSubtitles
                    )
                )
            )
        }.newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(parsed)
            .addToHistory()
        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = parsed
        )
            .addToHistory()
        val determined = determineCollectionEvents(collection = "MyCollection", convert.last(), TaskStatus.Failed)
            .addToHistory()
        val skippedCoverDownload = CoverDownloadSkippedEvent().derivedOf(determined.last())
            .addToHistory()

        val result = listener.onEvent(history.last(), history)
        assertThat(result).isInstanceOf(CollectedEvent::class.java)
    }



    @Test
    @DisplayName(
        """
        Hvis vi har kun encoded og extracted hendelser, men vi har sagt at vi også skal konvertere
        Når extract result kommer inn
        Så:
            Skal vi si pending på convert
            Listener skal returnere null
    """
    )
    fun failure1() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()

        val encode = encodeEvent("/tmp/video.mp4", parsed)
            .addToHistory()

        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
            .addToHistory()

        val result = listener.onEvent(history.last(), history)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName(
        """
        Hvis historikken har alle påkrevde media hendelser, men venter på metadata
        Når onEvent kalles og projeksjonen tilsier ugyldig tilstand
        Så:
            Returnerer vi null
    """
    )
    fun failure2() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(parsed).first()
            .addToHistory()

        val encode = encodeEvent("/tmp/video.mp4", parsed)
            .addToHistory()

        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
            .addToHistory()

        val convert = convertEvent(language = "en", baseName = "sub1", outputFiles = listOf("/tmp/sub1.vtt"), derivedFrom = extract.last())
            .addToHistory()

        val result = listener.onEvent(history.last(), history)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName(
        """
        Hvis historikken har alle påkrevde hendelser og encode feilet
        Når onEvent kalles og projeksjonen tilsier ugyldig tilstand
        Så:
            Collect returnerer null
    """
    )
    fun failure3() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(parsed)
            .addToHistory()

        val encode = encodeEvent("/tmp/video.mp4", parsed, TaskStatus.Failed)
            .addToHistory()

        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
            .addToHistory()

        val convert = convertEvent(language = "en", baseName = "sub1", outputFiles = listOf("/tmp/sub1.vtt"), derivedFrom = extract.last())
            .addToHistory()

        val cover = coverEvent("/tmp/cover.jpg", metadata.last())
            .addToHistory()

        val result = listener.onEvent(history.last(), history)

        assertThat(result).isNull()
    }


    @Test
    @DisplayName(
        """
        Hvis ingen oppgaver har blitt gjort
        Når onEvent kalles
        Så:
            Skal projeksjonen gi ugyldig tilstand og returnere null
    """
    )
    fun failure4() {
        val started = defaultStartEvent().let { ev ->
            ev.copy(data = ev.data.copy(operation = setOf(OperationType.Encode)))
        }.newReferenceId()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()


        val result = listener.onEvent(history.last(), history)

        assertThat(result).isNull()
    }

    @Test
    @DisplayName(
        """
        Summarizer skal være idempotent:
        - Første kjøring skal produsere CollectedEvent
        - Andre kjøring skal returnere null
        - Re-feed skal ikke produsere flere CollectedEvent
        """
    )
    fun summarizerDoesNotGoHaywire() {
        val started = defaultStartEvent()
            .addToHistory()

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)
            .addToHistory()

        val metadata = metadataEvent(parsed)
            .addToHistory()

        val encode = encodeEvent("/tmp/video.mp4", parsed)
            .addToHistory()

        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
            .addToHistory()

        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = extract.last()
        )
            .addToHistory()

        val cover = coverEvent("/tmp/cover.jpg", metadata.last())
            .addToHistory()
        val determined = determineCollectionEvents(collection = "MyCollection", cover.last())
            .addToHistory()


        // Første kjøring: skal produsere CollectedEvent
        val first = listener.onEvent(history.last(), history)
        assertThat(first).isInstanceOf(CollectedEvent::class.java)

        // Simuler at summarizer-eventet ble lagret i historikken
        val collected = first as CollectedEvent
        val newHistory = history + collected
        eventStore.setHistory(newHistory)

        // Andre kjøring: summarizer skal se at CollectedEvent finnes → returnere null
        val second = listener.onEvent(collected, newHistory)
        assertThat(second).isNull()

        // Tredje kjøring: re-feed av siste event → fortsatt null
        val third = listener.onEvent(collected, newHistory)
        assertThat(third).isNull()
    }

    @Test
    @DisplayName("""
        Når ConvertSubtitles er eneste operasjon
        Hvis StartFlow er Manual og konvertering fullføres
        Så:
            Skal lytteren samle resultatet og returnere CollectedEvent
    """)
    fun convertOnlyShouldCollectOnManual() {
        val started = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ConvertSubtitles),
                flow = StartFlow.Manual,
                fileUri = "/tmp/sub1.srt",
            )
        ).newReferenceId()
            .addToHistory()

        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = started
        )
            .addToHistory()
        val determined = determineCollectionEvents(collection = "MyCollection", convert.last())
            .addToHistory()
        val skippedCoverDownload = CoverDownloadSkippedEvent().derivedOf(determined.last())
            .addToHistory()

        val result = listener.onEvent(convert.last(), history)
        assertNotNull(result)
        assertThat(result).isInstanceOf(CollectedEvent::class.java)

    }

    @Test
    @DisplayName("""
        Når ConvertSubtitles er eneste operasjon
        Hvis StartFlow er Auto og konvertering fullføres
        Så:
            Skal lytteren samle resultatet og returnere CollectedEvent
    """)
    fun convertOnlyShouldCollectOnAuto() {
        val started = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ConvertSubtitles),
                flow = StartFlow.Auto,
                fileUri = "/tmp/sub1.srt",
            )
        ).newReferenceId()
            .addToHistory()

        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = started
        )
            .addToHistory()
        val determined = determineCollectionEvents(collection = "MyCollection", convert.last())
            .addToHistory()
        val skippedCoverDownload = CoverDownloadSkippedEvent().derivedOf(determined.last())
            .addToHistory()
        val result = listener.onEvent(convert.last(), history)
        assertNotNull(result)
        assertThat(result).isInstanceOf(CollectedEvent::class.java)

    }

    @Test
    @DisplayName("""
        Når ConvertSubtitles ikke er eneste operasjon
        Hvis flere operasjoner er angitt i StartEvent
        Så:
            Skal lytteren ikke samle resultatet og returnere null
    """)

    fun convertWithMoreOperationsShouldNotCollect() {
        val started = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ConvertSubtitles, OperationType.ExtractSubtitles),
                flow = StartFlow.Auto,
                fileUri = "/tmp/sub1.srt",
            )
        ).newReferenceId()
            .addToHistory()

        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = started
        )
            .addToHistory()

        val result = listener.onEvent(convert.last(), history)
        assertNull(result)
    }

}