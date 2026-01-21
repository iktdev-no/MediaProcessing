package no.iktdev.mediaprocessing.coordinator.listeners.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

import io.mockk.*
import no.iktdev.eventi.models.Event
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import java.nio.file.Files
import java.nio.file.Path

class MediaCreateConvertTaskListenerTest : TestBase() {

    private val listener = MediaCreateConvertTaskListener()

    @Test
    @DisplayName("""
        Når en ProcesserExtractResultEvent mottas
        Hvis historikken inneholder StartProcessingEvent med Convert og filen eksisterer
        Så:
            Skal det opprettes ConvertTask og returneres ConvertTaskCreatedEvent
    """)
    fun verifyConvertTaskCreatedOnValidHistory() {
        val tempFile = File.createTempFile("test", ".srt")
        tempFile.writeText("dummy subtitle")

        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = tempFile.absolutePath,
                operation = setOf(OperationType.ConvertSubtitles)
            )
        )
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        )

        val history = listOf(startEvent)
        val result = listener.onEvent(extractEvent, history)

        assertNotNull(result)
        assertTrue(result is ConvertTaskCreatedEvent)

        // verifiser at TaskStore.persist ble kalt med ConvertTask
        verify { TaskStore.persist(match { it is ConvertTask }) }
    }

    @Test
    @DisplayName("""
        Når en ProcesserExtractResultEvent mottas
        Hvis StartProcessingEvent mangler i historikken
        Så:
            Skal onEvent returnere null og TaskStore.persist ikke kalles
    """)
    fun verifyNullWhenNoStartEvent() {
        val tempFile = File.createTempFile("test", ".srt")
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        )

        val history = emptyList<Event>()
        val result = listener.onEvent(extractEvent, history)

        assertNull(result)
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName("""
        Når en ProcesserExtractResultEvent mottas
        Hvis StartProcessingEvent finnes men operation ikke inneholder Convert
        Så:
            Skal onEvent returnere null
    """)
    fun verifyNullWhenOperationNotConvert() {
        val tempFile = File.createTempFile("test", ".srt")
        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = tempFile.absolutePath,
                operation = setOf(OperationType.Encode) // Ikke Convert
            )
        )
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        )

        val history = listOf(startEvent)
        val result = listener.onEvent(extractEvent, history)

        assertNull(result)
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName("""
        Når en ProcesserExtractResultEvent mottas
        Hvis status ikke er Completed
        Så:
            Skal onEvent returnere null
    """)
    fun verifyNullWhenStatusNotCompleted() {
        val tempFile = File.createTempFile("test", ".srt")
        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = tempFile.absolutePath,
                operation = setOf(OperationType.ConvertSubtitles)
            )
        )
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Failed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        )

        val history = listOf(startEvent)
        val result = listener.onEvent(extractEvent, history)

        assertNull(result)
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName("""
        Når en ProcesserExtractResultEvent mottas
        Hvis data mangler (er null)
        Så:
            Skal onEvent returnere null
    """)
    fun verifyNullWhenDataIsNull() {
        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = "video.mp4",
                operation = setOf(OperationType.ConvertSubtitles)
            )
        )
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = null
        )

        val history = listOf(startEvent)
        val result = listener.onEvent(extractEvent, history)

        assertNull(result)
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName("""
        Når en ProcesserExtractResultEvent mottas
        Hvis cachedOutputFile ikke eksisterer
        Så:
            Skal onEvent returnere null
    """)
    fun verifyNullWhenFileDoesNotExist() {
        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = "nonexistent.srt",
                operation = setOf(OperationType.ConvertSubtitles)
            )
        )
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = "nonexistent.srt",
                language = "en"
            )
        )

        val history = listOf(startEvent)
        val result = listener.onEvent(extractEvent, history)

        assertNull(result)
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName("""
        Når en ProcesserExtractResultEvent mottas
        Hvis historikken inneholder StartEvent med Convert og File.exists() returnerer true
        Så:
            Skal det opprettes ConvertTask og returneres ConvertTaskCreatedEvent
    """)
    fun verifyConvertTaskCreatedWithMockedFileExists() {
        // Intercept File konstruktør og mock exists()
        mockStatic(Files::class.java).use { filesMock ->

            filesMock.`when`<Boolean> {
                Files.exists(any<Path>())
            }.thenReturn(true)

            val startEvent = StartProcessingEvent(
                data = StartData(
                    fileUri = "/tmp/video.srt",
                    operation = setOf(OperationType.ConvertSubtitles)
                )
            )

            val extractEvent = ProcesserExtractResultEvent(
                status = TaskStatus.Completed,
                data = ProcesserExtractResultEvent.ExtractResult(
                    cachedOutputFile = "/tmp/video.srt",
                    language = "en"
                )
            )

            val history = listOf(startEvent)
            val result = listener.onEvent(extractEvent, history)

            assertNotNull(result)
            assertTrue(result is ConvertTaskCreatedEvent)

            filesMock.verify {
                Files.exists(any<Path>())
            }
        }
    }
}

