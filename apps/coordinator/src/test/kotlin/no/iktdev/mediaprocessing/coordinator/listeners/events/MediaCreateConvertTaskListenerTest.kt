package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.verify
import no.iktdev.eventi.events.SoftDispatchException
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.exfl.using
import no.iktdev.files.FakeFile
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

class MediaCreateConvertTaskListenerTest : TestBase() {

    private val listener = MediaCreateConvertTaskListener()

    @Test
    @DisplayName(
        """
        Når en ProcesserExtractResultEvent mottas
        Hvis historikken inneholder StartProcessingEvent med Convert og filen eksisterer
        Så:
            Skal det opprettes ConvertTask og returneres ConvertTaskCreatedEvent
    """
    )
    fun verifyConvertTaskCreatedOnValidHistory() {
        val tempFile = File.createTempFile("test", ".srt")
        tempFile.writeText("dummy subtitle")

        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = tempFile.absolutePath,
                operation = setOf(OperationType.ExtractSubtitles, OperationType.ConvertSubtitles)
            )
        ).newReferenceId()
            .addToHistory()
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        ).derivedOf(startEvent)
            .addToHistory()

        val result = listener.onEvent(extractEvent, history)

        assertNotNull(result)
        assertTrue(result is ConvertTaskCreatedEvent)

        // verifiser at TaskStore.persist ble kalt med ConvertTask
        verify { TaskStore.persist(match { it is ConvertTask }) }
    }

    @Test
    @DisplayName(
        """
        Når en ProcesserExtractResultEvent mottas
        Hvis StartProcessingEvent mangler i historikken
        Så:
            Skal onEvent returnere null og TaskStore.persist ikke kalles
    """
    )
    fun verifyNullWhenNoStartEvent() {
        val tempFile = File.createTempFile("test", ".srt")
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        ).newReferenceId()
            .addToHistory()

        val result = listener.onEvent(extractEvent, history)

        assertNull(result)
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName(
        """
        Når en ProcesserExtractResultEvent mottas
        Hvis StartProcessingEvent finnes men operation ikke inneholder Convert
        Så:
            Skal onEvent returnere null
    """
    )
    fun verifyNullWhenOperationNotConvert() {
        val tempFile = File.createTempFile("test", ".srt")
        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = tempFile.absolutePath,
                operation = setOf(OperationType.Encode) // Ikke Convert
            )
        ).newReferenceId()
            .addToHistory()
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        ).derivedOf(startEvent)
            .addToHistory()

        val result = listener.onEvent(extractEvent, history)

        assertNull(result)
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName(
        """
        Når en ProcesserExtractResultEvent mottas
        Hvis status ikke er Completed
        Så:
            Skal onEvent returnere null
    """
    )
    fun verifyNullWhenStatusNotCompleted() {
        val tempFile = File.createTempFile("test", ".srt")
        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = tempFile.absolutePath,
                operation = setOf(OperationType.ConvertSubtitles, OperationType.ExtractSubtitles)
            )
        ).newReferenceId()
            .addToHistory()
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Failed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        ).derivedOf(startEvent)
            .addToHistory()

        assertThrows<SoftDispatchException.ForcedListenerEjectionException> { listener.onEvent(extractEvent, history) }

        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName(
        """
        Når en ProcesserExtractResultEvent mottas
        Hvis data mangler (er null)
        Så:
            Skal onEvent returnere null
    """
    )
    fun verifyNullWhenDataIsNull() {

        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = "video.mp4",
                operation = setOf(OperationType.ExtractSubtitles, OperationType.ConvertSubtitles)
            )
        ).newReferenceId()
            .addToHistory()
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = null
        ).derivedOf(startEvent)
            .addToHistory()

        assertThrows<SoftDispatchException.ForcedListenerEjectionException> {
            listener.onEvent(extractEvent, history)
        }
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName(
        """
        Når en sekvens starter med inputfil som er av subtitle
        Hvis formatet er støttet
        Så:
            Skal onEvent returnere opprettet task
    """
    )
    fun verifyConvertCreatedIfOnlyConvert() {
        IFile.factory = { path -> FakeFile(path, exists = true) }

        val inFile = File("inbox").using("eng", "nonexistent.srt")
        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = inFile.absolutePath,
                operation = setOf(OperationType.ExtractSubtitles, OperationType.ConvertSubtitles)
            )
        ).newReferenceId()
            .addToHistory()
        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = inFile.absolutePath,
                language = "eng"
            )
        ).derivedOf(startEvent)
            .addToHistory()

        val result = listener.onEvent(extractEvent, history)
        assertNotNull(result)
        assertThat(result!!::class.java).isEqualTo(ConvertTaskCreatedEvent::class.java)

        verify(exactly = 1) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName(
        """
    Når en ProcesserExtractResultEvent mottas
    Hvis historikken inneholder StartEvent med Convert og IFile.exists() returnerer true
    Så:
        Skal det opprettes ConvertTask og returneres ConvertTaskCreatedEvent
"""
    )
    fun verifyConvertTaskCreatedWithMockedFileExists() {

        // Override IFile.factory for denne testen
        IFile.factory = { path ->
            FakeFile(path, exists = true)
        }

        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = "/tmp/video.srt",
                operation = setOf(OperationType.ConvertSubtitles, OperationType.ExtractSubtitles)
            )
        ).newReferenceId()
            .addToHistory()

        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = "/tmp/video.srt",
                language = "en"
            )
        ).derivedOf(startEvent)
            .addToHistory()

        val result = listener.onEvent(extractEvent, history)

        assertNotNull(result)
        assertTrue(result is ConvertTaskCreatedEvent)
    }


    @Test
    @DisplayName(
        """
        Når en StartProcessingEvent mottas
        Hvis operation kun er ConvertSubtitles
        Så:
            Skal direct flow brukes og ConvertTaskCreatedEvent returneres
    """
    )
    fun verifyDirectFlowWithoutExtractEvent() {
        val inFile = File("inbox").using("eng", "file.srt")

        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = inFile.absolutePath,
                operation = setOf(OperationType.ConvertSubtitles)
            )
        ).newReferenceId()
            .addToHistory()

        val result = listener.onEvent(startEvent, history)

        assertNotNull(result)
        assertTrue(result is ConvertTaskCreatedEvent)
        verify(exactly = 1) { TaskStore.persist(any()) }
    }

    @Test
    @DisplayName(
        """
    Når en StartProcessingEvent mottas
    Hvis operation kun er ConvertSubtitles men filen har ugyldig extension
    Så:
        Skal onEvent returnere null og ingen task opprettes
"""
    )
    fun verifyDirectFlowInvalidExtension() {
        val inFile = File("inbox").using("eng", "file.txt")

        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = inFile.absolutePath,
                operation = setOf(OperationType.ConvertSubtitles)
            )
        ).newReferenceId()
            .addToHistory()

        assertThrows<SoftDispatchException.ForcedListenerEjectionException> {
            listener.onEvent(startEvent, history)
        }
        verify(exactly = 0) { TaskStore.persist(any()) }
    }

    fun WorkingFolder() = File("build").using("test-run")


    @Test
    @DisplayName(
        """
    Når en ProcesserExtractResultEvent mottas
    Hvis operation inneholder ConvertSubtitles sammen med andre operasjoner
    Så:
        Skal normal flow fortsatt opprette ConvertTask
"""
    )
    fun verifyNormalFlowWithMultipleOperations() {
        val tempFile = File.createTempFile("test", ".srt").apply { writeText("dummy") }

        val startEvent = StartProcessingEvent(
            data = StartData(
                fileUri = tempFile.absolutePath,
                operation = setOf(
                    OperationType.ConvertSubtitles,
                    OperationType.Encode // ekstra operasjon
                )
            )
        ).newReferenceId()
            .addToHistory()

        val extractEvent = ProcesserExtractResultEvent(
            status = TaskStatus.Completed,
            data = ProcesserExtractResultEvent.ExtractResult(
                cachedOutputFile = tempFile.absolutePath,
                language = "en"
            )
        ).derivedOf(startEvent)
            .addToHistory()

        val result = listener.onEvent(extractEvent, history)

        assertNotNull(result)
        assertTrue(result is ConvertTaskCreatedEvent)

        verify(exactly = 1) { TaskStore.persist(any()) }
    }


}

