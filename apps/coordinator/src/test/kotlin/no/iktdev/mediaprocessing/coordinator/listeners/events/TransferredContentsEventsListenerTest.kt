package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.mockk
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskIdentity
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.MockData.convertEvent
import no.iktdev.mediaprocessing.MockData.coverEvent
import no.iktdev.mediaprocessing.MockData.encodeEvent
import no.iktdev.mediaprocessing.MockData.extractEvent
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.MockData.metadataEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.*
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.withMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class TransferredContentsEventsListenerTest : TestBase() {

    // Gjenbruker mock eller tom definisjon for EventStore siden logikken kjører mot fullHistory-listene
    private val listener = TransferredContentsEventsListener(eventStore)

    // ---------------------------------------------------------
    // Helper: Bygger Transfer-kjeder akkurat som i den andre testen din
    // ---------------------------------------------------------
    private fun createTransferResults(
        persist: Event,
        includeVideo: Boolean = false,
        includeCover: Boolean = false,
        subtitles: List<String> = emptyList(),
        status: TaskStatus = TaskStatus.Completed
    ): Pair<TransferContentTaskCreatedEvent, List<Event>> {

        val taskIds = mutableListOf<MultiTaskIdentity>()

        if (includeVideo) taskIds += MultiTaskIdentity(UUID.randomUUID(), "video")
        if (includeCover) taskIds += MultiTaskIdentity(UUID.randomUUID(), "cover")
        val resultList = mutableListOf<Event>()

        subtitles.forEach { lang ->
            taskIds += MultiTaskIdentity(UUID.randomUUID(), "sub-$lang")
        }

        val created = TransferContentTaskCreatedEvent(
            groupId = persist.eventId,
            taskIds = taskIds.toSet()
        ).derivedOf(persist) as TransferContentTaskCreatedEvent

        var idx = 0
        if (includeVideo) {
            resultList += VideoTransferredResultEvent(
                fileUri = "store:///video.mp4",
                collection = "Test",
                status = status,
                error = null
            ).derivedOf(created).apply {
                withMetadata(metadata.derivedFromEventId(taskIds[idx++].taskId))
            }
        }

        if (includeCover) {
            resultList += CoverTransferredResultEvent(
                fileUri = "store:///cover.jpg",
                collection = "Test",
                status = status,
                error = null
            ).derivedOf(created).apply {
                withMetadata(metadata.derivedFromEventId(taskIds[idx++].taskId))
            }
        }

        subtitles.forEach { lang ->
            resultList += SubtitleTransferredResultEvent(
                fileUri = "store:///sub-$lang.ass",
                collection = "Test",
                language = lang,
                status = status,
                error = null
            ).derivedOf(created).apply {
                withMetadata(metadata.derivedFromEventId(taskIds[idx++].taskId))
            }
        }

        return created to resultList
    }

    private fun setupBaseHistory(): Event {
        val started = defaultStartEvent().addToHistory()
        val parsed = mediaParsedEvent(collection = "Test", fileName = "File", mediaType = no.iktdev.mediaprocessing.shared.common.model.MediaType.Movie).derivedOf(started).addToHistory()
        val meta = metadataEvent(parsed).also { it.forEach { e -> e.addToHistory() } }
        val encode = encodeEvent("/tmp/video.mp4", meta.last()).also { it.forEach { e -> e.addToHistory() } }
        val collected = CollectedEvent(history.map { it.eventId }.toSet()).derivedOf(encode.last()).addToHistory()
        val summaryEvent = SummarizeContentListener(coordinatorEnv).onEvent(collected, history)!!.addToHistory()
        return PersistContentEvent().derivedOf(summaryEvent).addToHistory()
    }

    // ---------------------------------------------------------
    // TESTER
    // ---------------------------------------------------------

    @Test
    @DisplayName("shouldSummarize returnerer true og produserer summary når alle transfers er Completed")
    fun shouldSummarizeWhenAllCompleted() {
        val persistContent = setupBaseHistory()

        val (created, results) = createTransferResults(persistContent, includeVideo = true, includeCover = true)
        created.addToHistory()
        results.forEach { it.addToHistory() }

        // Sjekk tilstanden via projeksjonen
        val shouldSummarize = listener.shouldSummarize(history)
        assertThat(shouldSummarize).isTrue()

        // Sjekk at vi kan generere en summary-event
        val summary = listener.produceSummary(history)
        assertThat(summary).isInstanceOf(TransferredContentsSummaryEvent::class.java)

        val summaryEvent = summary as TransferredContentsSummaryEvent
        // Skal inneholde ID-ene til fil-eventene (og persist hvis koden krever det)
        val expectedIds = results.map { it.eventId }.toSet() + persistContent.eventId
        assertThat(summaryEvent.summarizedEventIds).containsAll(expectedIds)
    }

    @Test
    @DisplayName("shouldSummarize returnerer false når noen oppgaver fremdeles er Pending")
    fun shouldNotSummarizeWhenPending() {
        val persistContent = setupBaseHistory()

        // Opprett oppgaven, men ikke legg til resultatene i historikken (de er "under arbeid")
        val (created, _) = createTransferResults(persistContent, includeVideo = true)
        created.addToHistory()
        // results.forEach { it.addToHistory() } <-- Dropp denne!

        val shouldSummarize = listener.shouldSummarize(history)
        assertThat(shouldSummarize).isFalse()
    }

    @Test
    @DisplayName("summaryAlreadyExists returnerer true hvis nøyaktig samme gruppe har blitt oppsummert fra før")
    fun blocksDuplicateSummaryForSameGroup() {
        val persistContent = setupBaseHistory()

        val (created, results) = createTransferResults(persistContent, includeVideo = true, includeCover = true)
        created.addToHistory()
        results.forEach { it.addToHistory() }

        // Generer og legg til en eksisterende summary i historikken vår
        val existingSummary = listener.produceSummary(history).addToHistory()

        // Nå som nøyaktig samme tilstand (samme fil-ID-er) ligger der, skal summaryAlreadyExists kaste den ut
        val alreadyExists = listener.summaryAlreadyExists(history)
        assertThat(alreadyExists).isTrue()
    }

    @Test
    @DisplayName("Tillater å generere ny summary dersom en helt ny gruppe/bølge dukker opp i historikken")
    fun allowsNewSummaryForNewIncomingGroup() {
        val persistContent = setupBaseHistory()

        // --- Bølge 1: Video overføres ---
        val (created1, results1) = createTransferResults(persistContent, includeVideo = true)
        created1.addToHistory()
        results1.forEach { it.addToHistory() }

        // Bølge 1 blir ferdig, og det lages en summary for denne tilstanden
        val summary1 = listener.produceSummary(history).addToHistory()

        // Verifiser at bølge 1 nå er låst (skal returnere true på duplikat-sjekk)
        assertThat(listener.summaryAlreadyExists(history)).isTrue()

        // --- Bølge 2: Undertekster dukker opp som en ny gruppe senere ---
        val (created2, results2) = createTransferResults(persistContent, subtitles = listOf("eng"))
        created2.addToHistory()
        results2.forEach { it.addToHistory() }

        // Siden historikken nå har fått nye fil-event-IDer (results2),
        // skal den gamle oppsummeringen ikke lenger matche det totale settet av ID-er.
        val alreadyExistsForNewState = listener.summaryAlreadyExists(history)

        // Denne MÅ være false for at den nye bølgen skal slippe igjennom!
        assertThat(alreadyExistsForNewState).isFalse()

        // Verifiser at den nye summaryen produseres med absolutt alle ID-ene akkumulert
        val summary2 = listener.produceSummary(history) as TransferredContentsSummaryEvent
        val allExpectedIds = (results1 + results2).map { it.eventId }.toSet() + persistContent.eventId
        assertThat(summary2.summarizedEventIds).containsAll(allExpectedIds)
    }
}