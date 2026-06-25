package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreMediaInfoAndMetadataTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreMediaInfoAndMetadataTask
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.util.*

@ExtendWith(MockitoExtension::class)
class StoreMetadataAndReferencesTaskListenerTest {

    // -------------------------------------------------------------------------
    // Fake Reporter
    // -------------------------------------------------------------------------

    class FakeTaskReporter : TaskReporter {
        val events = mutableListOf<Event>()
        var completed = false
        var failed = false

        override fun markClaimed(taskId: UUID, workerId: String): Result { return Result.Success }
        override fun updateLastSeen(taskId: UUID): Result { return Result.Success }
        override fun markCompleted(taskId: UUID): Result { completed = true; return Result.Success }
        override fun markFailed(referenceId: UUID, taskId: UUID): Result { failed = true; return Result.Success }
        override fun markCancelled(referenceId: UUID, taskId: UUID): Result { return Result.Success }
        override fun updateProgress(referenceId: UUID, taskId: UUID, payload: Progress): Result { return Result.Success }
        override fun log(taskId: UUID, message: String) {}

        override fun publishEvent(event: Event): Result {
            events.add(event)
            return Result.Success
        }
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @Mock
    lateinit var restTemplate: RestTemplate

    lateinit var listener: StoreMetadataAndReferencesTaskListener

    @BeforeEach
    fun setup() {
        listener = StoreMetadataAndReferencesTaskListener()
        listener.streamitRestTemplate = restTemplate
    }

    private fun sampleContentExport(): ContentExport {
        return ContentExport(
            collection = "series",
            episodeInfo = ContentExport.EpisodeInfo(
                episodeNumber = 1,
                seasonNumber = 1,
                episodeTitle = "Pilot"
            ),
            media = ContentExport.MediaExport(
                videoFile = "bb.s01e01.mkv",
                subtitles = listOf(
                    ContentExport.MediaExport.Subtitle(
                        subtitleFile = "bb.en.srt",
                        language = "en"
                    )
                )
            ),
            metadata = ContentExport.MetadataExport(
                title = "Breaking Bad",
                genres = listOf("Drama"),
                cover = "bb.jpg",
                summary = emptyList(),
                mediaType = MediaType.Serie,
                source = "local"
            )
        )
    }

    // -------------------------------------------------------------------------
    // supports()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Gitt en StoreContentAndMetadataTask
        Når supports() kalles
        Så:
            Returnerer true
        """
    )
    fun supports_returnsTrueForCorrectTask() {
        val task = StoreMediaInfoAndMetadataTask(data = sampleContentExport())

        val result = listener.supports(task)

        assertThat(result).isTrue()
    }

    @Test
    @DisplayName(
        """
        Gitt en annen type Task
        Når supports() kalles
        Så:
            Returnerer false
        """
    )
    fun supports_returnsFalseForWrongTask() {
        val task = object : Task() {}

        val result = listener.supports(task)

        assertThat(result).isFalse()
    }

    // -------------------------------------------------------------------------
    // accept() — full TaskListener flow
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Gitt at RestTemplate returnerer 200 OK
        Når accept() kjøres
        Så:
            Publiseres Completed-event
        """
    )
    fun accept_returnsCompletedOnSuccess() = runTest {
        val reporter = FakeTaskReporter()
        val task = StoreMediaInfoAndMetadataTask(data = sampleContentExport()).newReferenceId()

        whenever(
            restTemplate.exchange(
                eq("/api/mediaprocesser/import"),
                eq(HttpMethod.POST),
                any(),
                eq(Void::class.java)
            )
        ).thenReturn(ResponseEntity(HttpStatus.OK))

        listener.accept(task, reporter)
        listener.currentJob?.join()

        val event = reporter.events.first() as StoreMediaInfoAndMetadataTaskResultEvent

        assertThat(reporter.completed).isTrue()
        assertThat(event.status).isEqualTo(TaskStatus.Completed)
    }

    @Test
    @DisplayName(
        """
        Gitt at RestTemplate kaster exception
        Når accept() kjøres
        Så:
            Publiseres Failed-event via createIncompleteStateTaskEvent()
        """
    )
    fun accept_returnsFailedOnException() = runTest {
        val reporter = FakeTaskReporter()
        val task = StoreMediaInfoAndMetadataTask(data = sampleContentExport()).newReferenceId()

        whenever(
            restTemplate.exchange(
                any<String>(),
                any(),
                any(),
                eq(Void::class.java)
            )
        ).thenThrow(RuntimeException("boom"))

        listener.accept(task, reporter)
        listener.currentJob?.join()

        val event = reporter.events.first() as StoreMediaInfoAndMetadataTaskResultEvent

        assertThat(reporter.failed).isTrue()
        assertThat(event.status).isEqualTo(TaskStatus.Failed)
        assertThat(event.error).contains("boom")
    }

    // -------------------------------------------------------------------------
    // workerId()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Gitt en gyldig task
        Når getWorkerId() kalles
        Så:
            Returnerer en streng som inneholder klassenavn, tasktype og UUID
        """
    )
    fun workerId_hasCorrectFormat() {
        val id = listener.getWorkerId()

        assertThat(id).contains("StoreMetadataAndReferencesTaskListener-MIXED-")
        assertThat(id.split("-").last().length).isGreaterThan(10)
    }
}
