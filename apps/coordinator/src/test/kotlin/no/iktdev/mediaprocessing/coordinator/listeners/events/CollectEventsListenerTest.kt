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

    private val listener = CollectEventsListener()

    @Test
    @DisplayName(
        """
            Hvis historikken har alle påkrevde hendelser og alle oppgaver er i en gyldig tisltand
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

        val result = listener.onEvent(history.last(), history)

        assertThat(result).isNotNull()
        assertThat {
            result is CollectedEvent
        }
    }


    @Test
    @DisplayName(
        """
        Hvis vi har kun encoded hendelse, men vi har sagt at vi også skal ha extract, men ikke har opprettet extract
        Når encode result kommer inn
        Så:
            Opprettes CollectEvent basert på historikken
        """
    )
    fun success2() {
        val started = defaultStartEvent().let { ev ->
            ev.copy(
                data = ev.data.copy(
                    operation = setOf(
                        OperationType.Metadata,
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
                        OperationType.Metadata,
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
                Listener skal returnerere null
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
        val result = listener.onEvent(history.last(), history)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName(
        """
            Hvis historikken har alle påkrevde media hendelser, men venter på metadata
            Når onEvent kalles og projeksjonen tilsier ugyldig tilstand
            Så:
                Returerer vi failure
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

        val result = listener.onEvent(history.last(), history)

        assertThat(result).isNull()
    }


    @Test
    @DisplayName(
        """
            Hvis historikken har alle påkrevde hendelser og encode feilet
            Når onEvent kalles og projeksjonen tilsier ugyldig tilstand
            Så:
                Collect feiler
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

        val result = listener.onEvent(history.last(), history)

        assertThat(result).isNull()
    }


}