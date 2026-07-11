package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskIdentity
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.MockData.convertEvent
import no.iktdev.mediaprocessing.MockData.coverEvent
import no.iktdev.mediaprocessing.MockData.encodeEvent
import no.iktdev.mediaprocessing.MockData.extractEvent
import no.iktdev.mediaprocessing.MockData.mediaParsedEvent
import no.iktdev.mediaprocessing.MockData.metadataEvent
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreMediaInfoAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import no.iktdev.mediaprocessing.withMetadata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*

class StoreMetadataAndReferencesListenerTest : TestBase() {

    private val listener = StoreMetadataAndReferencesListener()

    // ⭐ Instansierer collectoren for å lage det påkrevde samle-eventet
    private val transferCollector = TransferredContentsEventsListener(mockk(relaxed = true))

    // ---------------------------------------------------------
    // Helper: lager TransferContentTaskCreatedEvent + resultater
    // ---------------------------------------------------------

    private fun createTransferResults(
        persist: Event,
        includeVideo: Boolean = false,
        includeCover: Boolean = false,
        subtitles: List<String> = emptyList()
    ): Pair<TransferContentTaskCreatedEvent, List<Event>> {

        val taskIds = mutableListOf<MultiTaskIdentity>()

        if (includeVideo) taskIds += MultiTaskIdentity(UUID.randomUUID(), "video")
        if (includeCover) taskIds += MultiTaskIdentity(UUID.randomUUID(), "cover")
        subtitles.forEach { lang ->
            taskIds += MultiTaskIdentity(UUID.randomUUID(), "sub-$lang")
        }

        val created = TransferContentTaskCreatedEvent(
            groupId = persist.eventId,
            taskIds = taskIds.toSet()
        ).derivedOf(persist) as TransferContentTaskCreatedEvent

        val results = mutableListOf<Event>()
        var idx = 0

        if (includeVideo) {
            results += VideoTransferredResultEvent(
                fileUri = "store:///video.mp4",
                collection = "Baking Bread",
                status = TaskStatus.Completed,
                error = null
            ).derivedOf(created).apply {
                withMetadata(metadata.derivedFromEventId(taskIds[idx++].taskId))
            }
        }

        if (includeCover) {
            results += CoverTransferredResultEvent(
                fileUri = "store:///cover.jpg",
                collection = "Baking Bread",
                status = TaskStatus.Completed,
                error = null
            ).derivedOf(created).apply {
                withMetadata(metadata.derivedFromEventId(taskIds[idx++].taskId))
            }
        }

        subtitles.forEach { lang ->
            results += SubtitleTransferredResultEvent(
                fileUri = "store:///sub-$lang.ass",
                collection = "Baking Bread",
                language = lang,
                status = TaskStatus.Completed,
                error = null
            ).derivedOf(created).apply {
                withMetadata(metadata.derivedFromEventId(taskIds[idx++].taskId))
            }
        }

        return created to results
    }


    // ---------------------------------------------------------
    // TEST 1: Full pipeline (video + cover + subtitles)
    // ---------------------------------------------------------

    @Test
    @DisplayName("Oppretter StoreContentAndMetadataTask når alle transfer-tasks er fullført")
    fun createsTaskWhenAllTransfersCompleted() {

        val started = defaultStartEvent().addToHistory()

        val parsed = mediaParsedEvent(
            collection = "Baking Bread",
            fileName = "Baking Bread - S01E01 - Flour",
            mediaType = MediaType.Serie
        ).derivedOf(started).addToHistory()

        val metadata = metadataEvent(parsed).also { it.forEach { e -> e.addToHistory() } }

        val encode = encodeEvent("/tmp/video.mp4", metadata.last())
            .also { it.forEach { e -> e.addToHistory() } }

        val extract = extractEvent("eng", "/tmp/sub1.srt", encode.last())
            .also { it.forEach { e -> e.addToHistory() } }

        val coverDownload = coverEvent("/tmp/cover.jpg", metadata.last())
            .also { it.forEach { e -> e.addToHistory() } }

        val convert = convertEvent(
            language = "eng",
            baseName = "sub1",
            outputFiles = listOf("/tmp/sub1.ass"),
            derivedFrom = extract.last()
        ).also { it.forEach { e -> e.addToHistory() } }

        val collected = CollectedEvent(
            history.map { it.eventId }.toSet()
        ).derivedOf(convert.last()).addToHistory()

        val summaryEvent = SummarizeContentListener(coordinatorEnv)
            .onEvent(collected, history)!!.addToHistory()

        val persistContent = PersistContentEvent()
            .derivedOf(summaryEvent).addToHistory()

        // NY MODELL: TransferContentTaskCreatedEvent + resultater
        val (created, results) = createTransferResults(
            persist = persistContent,
            includeVideo = true,
            includeCover = true,
            subtitles = listOf("eng")
        )
        created.addToHistory()
        results.forEach { it.addToHistory() }

        // ⭐ NYTT: Vi lar collectoren generere TransferredContentsSummaryEvent og dytter det inn i historikken
        transferCollector.produceSummary(history).addToHistory()

        // Kjører listeneren med den oppdaterte historikken der samle-eventet nå ligger klart
        val result = listener.onEvent(history.last(), history)

        assertThat(result).isInstanceOf(StoreMediaInfoAndMetadataTaskCreatedEvent::class.java)

        val slot = slot<StoreMediaInfoAndMetadataTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured

        assertThat(storeTask.data.collection).isEqualTo("Baking Bread")
        assertThat(storeTask.data.media?.videoFile).isEqualTo("Baking Bread - S01E01 - Flour.mp4")
        assertThat { storeTask.data.media?.subtitles?.any { it.subtitleFile == "Baking Bread - S01E01 - Flour.ass"  } }
        assertThat(storeTask.data.media?.subtitles?.first()?.subtitleFile)
        assertThat(storeTask.data.media?.subtitles?.first()?.language).isEqualTo("eng")
    }

    // ---------------------------------------------------------
    // TEST 2: Kun subtitles
    // ---------------------------------------------------------

    @Test
    fun createsTaskWhenOnlySubtitlesTransferred() {

        val workFolder = IFile("build").using("subby", "sub", "eng")

        val started = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ConvertSubtitles),
                flow = StartFlow.Manual,
                fileUri = workFolder.using("subby.srt").absolutePath,
            )
        ).newReferenceId().addToHistory()

        val convert = convertEvent(
            language = "eng",
            baseName = "subby",
            outputFiles = listOf(workFolder.using("subby.vtt").absolutePath),
            derivedFrom = started
        ).addToHistory()

        val collected = CollectedEvent(
            history.map { it.eventId }.toSet()
        ).derivedOf(convert.last()).addToHistory()

        val summaryEvent = SummarizeContentListener(coordinatorEnv)
            .onEvent(collected, history)!!.addToHistory()

        val persistContent = PersistContentEvent()
            .derivedOf(summaryEvent).addToHistory()

        val (created, results) = createTransferResults(
            persist = persistContent,
            includeVideo = false,
            includeCover = false,
            subtitles = listOf("eng")
        )
        created.addToHistory()
        results.forEach { it.addToHistory() }

        // ⭐ NYTT: Genererer TransferredContentsSummaryEvent også for undertekst-caset
        transferCollector.produceSummary(history).addToHistory()

        val result = listener.onEvent(history.last(), history)

        assertThat(result).isInstanceOf(StoreMediaInfoAndMetadataTaskCreatedEvent::class.java)

        val slot = slot<StoreMediaInfoAndMetadataTask>()

        verify(exactly = 1) {
            TaskStore.persist(capture(slot))
        }

        val storeTask = slot.captured

        assertThat(storeTask.data.collection).isEqualTo("subby")
        assertThat(storeTask.data.media?.videoFile).isNull()
        assertThat(storeTask.data.media?.subtitles?.first()?.subtitleFile).isEqualTo("subby.vtt")
        assertThat(storeTask.data.media?.subtitles?.first()?.language).isEqualTo("eng")
        assertThat(storeTask.data.metadata).isNull()
    }
}