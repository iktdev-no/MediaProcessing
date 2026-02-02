package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.MockData.convertEvent
import no.iktdev.mediaprocessing.MockData.coverEvent
import no.iktdev.mediaprocessing.MockData.encodeEvent
import no.iktdev.mediaprocessing.MockData.extractEvent
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.MockData.metadataEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CollectedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.assertj.core.api.Assertions.assertThat
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

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)

        val metadata = metadataEvent(parsed)

        val encode = encodeEvent("/tmp/video.mp4", parsed)
        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
        val convert = convertEvent(language = "en", baseName = "sub1", outputFiles = listOf("/tmp/sub1.vtt"), derivedFrom = extract.last())
        val cover = coverEvent("/tmp/cover.jpg", metadata.last())

        val history = listOf(
            started,
            parsed,
            *metadata.toTypedArray(),
            *encode.toTypedArray(),
            *extract.toTypedArray(),
            *convert.toTypedArray(),
            *cover.toTypedArray(),
        )
        eventStore.setHistory(history)
        val result = listener.onEvent(history.last(), history)

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

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)

        val metadata = metadataEvent(parsed).first()
        val encode = encodeEvent("/tmp/video.mp4", parsed)

        val history = listOf(
            started,
            parsed,
            metadata,
            *encode.toTypedArray(),
        )
        eventStore.setHistory(history)

        val result = listener.onEvent(history.last(), history)

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

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)

        val metadata = metadataEvent(parsed)
        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = parsed
        )

        val history = listOf(
            started,
            parsed,
            *metadata.toTypedArray(),
            *convert.toTypedArray(),
        )
        eventStore.setHistory(history)

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

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)

        val encode = encodeEvent("/tmp/video.mp4", parsed)
        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())

        val history = listOf(
            started,
            parsed,
            *encode.toTypedArray(),
            *extract.toTypedArray(),
        )
        eventStore.setHistory(history)

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

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)

        val metadata = metadataEvent(parsed).first()
        val encode = encodeEvent("/tmp/video.mp4", parsed)
        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
        val convert = convertEvent(language = "en", baseName = "sub1", outputFiles = listOf("/tmp/sub1.vtt"), derivedFrom = extract.last())

        val history = listOf(
            started,
            parsed,
            metadata,
            *encode.toTypedArray(),
            *extract.toTypedArray(),
            *convert.toTypedArray(),
        )
        eventStore.setHistory(history)

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

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)

        val metadata = metadataEvent(parsed)

        val encode = encodeEvent("/tmp/video.mp4", parsed, TaskStatus.Failed)
        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
        val convert = convertEvent(language = "en", baseName = "sub1", outputFiles = listOf("/tmp/sub1.vtt"), derivedFrom = extract.last())
        val cover = coverEvent("/tmp/cover.jpg", metadata.last())

        val history = listOf(
            started,
            parsed,
            *metadata.toTypedArray(),
            *encode.toTypedArray(),
            *extract.toTypedArray(),
            *convert.toTypedArray(),
            *cover.toTypedArray(),
        )
        eventStore.setHistory(history)

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

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)

        val history = listOf(
            started,
            parsed,
        )
        eventStore.setHistory(history)

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

        val parsed = mediaParsedEvent(
            collection = "MyCollection",
            fileName = "MyCollection 1",
            mediaType = MediaType.Movie
        ).derivedOf(started)

        val metadata = metadataEvent(parsed)
        val encode = encodeEvent("/tmp/video.mp4", parsed)
        val extract = extractEvent("en", "/tmp/sub1.srt", encode.last())
        val convert = convertEvent(
            language = "en",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.vtt"),
            derivedFrom = extract.last()
        )
        val cover = coverEvent("/tmp/cover.jpg", metadata.last())

        val history = listOf(
            started,
            parsed,
            *metadata.toTypedArray(),
            *encode.toTypedArray(),
            *extract.toTypedArray(),
            *convert.toTypedArray(),
            *cover.toTypedArray(),
        )

        // Gi summarizeren full historikk
        eventStore.setHistory(history)

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


}