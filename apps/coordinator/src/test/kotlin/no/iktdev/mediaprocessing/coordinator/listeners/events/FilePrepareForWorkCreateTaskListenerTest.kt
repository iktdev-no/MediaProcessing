package no.iktdev.mediaprocessing.coordinator.listeners.events

import io.mockk.slot
import io.mockk.verify
import no.iktdev.eventi.events.SoftDispatchException
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.coordinator.listeners.tasks.FilePrepareForWorkTaskListener
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.FilePrepareForWorkTaskCreatedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ValidateFileAndMediaDataEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.FilePrepareForWorkTask
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.capture
import org.mockito.kotlin.verify
import java.io.File

class FilePrepareForWorkCreateTaskListenerTest: TestBase() {

    val listener = FilePrepareForWorkCreateTaskListener(coordinatorEnv)

    @Test
    @DisplayName("""
    Når event ikke er av typen ValidateFileAndMediaDataEvent
    Hvis onEvent kalles
    Så:
        Kastes UnqualifiedEntryEventException
""")
    fun testWrongEntryEventThrows() {
        assertThrowsExactly(
            SoftDispatchException.UnqualifiedEntryEventException::class.java
        ) {
            listener.onEvent(DummyEvent(), history)
        }
    }

    @Test
    @DisplayName("""
    Når ValidateFileAndMediaDataEvent har ValidationStatus != Ok
    Hvis onEvent kalles
    Så:
        Returneres null
""")
    fun testValidationNotOkReturnsNull() {
        val event = ValidateFileAndMediaDataEvent(
            validationStatus = ValidateFileAndMediaDataEvent.ValidationStatus.Rejected,
            warnings = emptyList()
        )

        val result = listener.onEvent(event, emptyList())

        assertNull(result)
    }

    @Test
    @DisplayName("""
    Når StartProcessingEvent ikke inneholder Encode eller ExtractSubtitles
    Hvis onEvent kalles
    Så:
        Returneres null
""")
    fun testOperationTypeNotSupportedReturnsNull() {
        val start = StartProcessingEvent(
            StartData(
                operation = setOf(OperationType.MetadataSearch),   // ikke støttet
                fileUri = "/tmp/movie.mkv"
            )
        ).newReferenceId().addToHistory()

        val validated = ValidateFileAndMediaDataEvent(
            validationStatus = ValidateFileAndMediaDataEvent.ValidationStatus.Ok,
            warnings = emptyList()
        ).derivedOf(start)

        val result = listener.onEvent(validated, listOf(start))

        assertNull(result)
    }

    @Test
    @DisplayName("""
    Når ValidateFileAndMediaDataEvent er Ok
    Og StartProcessingEvent inneholder Encode
    Og kildefilen eksisterer
    Når onEvent kalles
    Så:
        Opprettes FilePrepareForWorkTask
        TaskStore.persist kalles med riktig data
        Returneres FilePrepareForWorkTaskCreatedEvent
""")
    fun testCreatesTaskSuccessfully() {
        // Arrange
        val sourceFile = File("build/test-intermediate/input.mkv")
        sourceFile.parentFile.mkdirs()
        sourceFile.writeText("dummy")   // sørg for at filen finnes

        coordinatorEnv.scratchFolder.mkdirs()

        val start = StartProcessingEvent(
            StartData(
                operation = setOf(OperationType.Encode),
                fileUri = sourceFile.path
            )
        ).newReferenceId().addToHistory()

        val validated = ValidateFileAndMediaDataEvent(
            validationStatus = ValidateFileAndMediaDataEvent.ValidationStatus.Ok,
            warnings = emptyList()
        ).derivedOf(start)

        // Act
        val result = listener.onEvent(validated, listOf(start))

        // Assert – tasken som ble sendt til TaskStore
        val slot = slot<FilePrepareForWorkTask>()
        verify(exactly = 1) { TaskStore.persist(capture(slot)) }

        val data = slot.captured.data

        assertEquals(sourceFile.absolutePath, data.sourceFile)
        assertTrue(data.destinationFile.contains("scratch"))
        assertTrue(data.destinationFile.endsWith("input.mkv"))

        // Assert – retur-eventet
        assertTrue(result is FilePrepareForWorkTaskCreatedEvent)
        assertEquals(slot.captured.taskId, (result as FilePrepareForWorkTaskCreatedEvent).taskId)
    }



}