package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StoreContentAndMetadataTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.StoreContentAndMetadataTask
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

@ExtendWith(MockitoExtension::class)
class StoreContentAndMetadataTaskListenerTest {

    @Mock
    lateinit var restTemplate: RestTemplate

    lateinit var listener: StoreContentAndMetadataTaskListener

    @BeforeEach
    fun setup() {
        listener = StoreContentAndMetadataTaskListener()
        listener.streamitRestTemplate = restTemplate
    }

    private fun sampleContentExport(): ContentExport {
        return ContentExport(
            collection = "series",
            episodeInfo = ContentExport.EpisodeInfo(episodeNumber = 1, seasonNumber = 1, episodeTitle = "Pilot"),
            media = ContentExport.MediaExport(
                videoFile = "bb.s01e01.mkv",
                subtitles = listOf(ContentExport.MediaExport.Subtitle(subtitleFile = "bb.en.srt", language = "en"))
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
        val task = StoreContentAndMetadataTask(data = sampleContentExport())

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

    @Test
    @DisplayName(
        """
        Gitt at RestTemplate returnerer 200 OK
        Når onTask() kalles
        Så:
            Returnerer Completed-event
    """
    )
    fun onTask_returnsCompletedOnSuccess() = runTest {
        val task = StoreContentAndMetadataTask(data = sampleContentExport())

        whenever(
            restTemplate.exchange(
                eq("open/api/mediaprocesser/import"),
                eq(HttpMethod.POST),
                any(),
                eq(Void::class.java)
            )
        ).thenReturn(ResponseEntity(HttpStatus.OK))

        val event = listener.onTask(task)

        assertThat(event).isInstanceOf(StoreContentAndMetadataTaskResultEvent::class.java)
        val result = event as StoreContentAndMetadataTaskResultEvent
        assertThat(result.status).isEqualTo(TaskStatus.Completed)
    }

    @Test
    @DisplayName(
        """
        Gitt at RestTemplate kaster exception
        Når onTask() kalles
        Så:
            Returnerer Failed-event
    """
    )
    fun onTask_returnsFailedOnException() = runTest {
        val task = StoreContentAndMetadataTask(data = sampleContentExport())

        whenever(
            restTemplate.exchange(
                any<String>(),
                any(),
                any(),
                eq(Void::class.java)
            )
        ).thenThrow(RuntimeException("boom"))

        val event = listener.onTask(task)

        assertThat(event).isInstanceOf(StoreContentAndMetadataTaskResultEvent::class.java)
        val result = event as StoreContentAndMetadataTaskResultEvent
        assertThat(result.status).isEqualTo(TaskStatus.Failed)
    }

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

        assertThat(id).contains("StoreContentAndMetadataTaskListener-MIXED-")
        assertThat(id.split("-").last().length).isGreaterThan(10) // UUID-ish
    }
}
