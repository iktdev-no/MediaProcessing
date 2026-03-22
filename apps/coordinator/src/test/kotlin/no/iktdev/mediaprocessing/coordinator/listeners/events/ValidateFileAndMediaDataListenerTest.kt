package no.iktdev.mediaprocessing.coordinator.listeners.events

import com.google.gson.Gson
import com.google.gson.JsonObject
import no.iktdev.eventi.events.SoftDispatchException
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.files.FakeFile
import no.iktdev.mediaprocessing.MockData.dummyAudioStream
import no.iktdev.mediaprocessing.MockData.dummyVideoStream
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.defaultMediaStreamParsedEvent
import no.iktdev.mediaprocessing.ffmpeg.data.ParsedMediaStreams
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaStreamParsedEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ValidateFileAndMediaDataEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

class ValidateFileAndMediaDataListenerTest: TestBase() {

    val listener = ValidateFileAndMediaDataListener()

    fun parse(value: String): JsonObject {
        return Gson().fromJson(value, JsonObject::class.java)
    }

    @Test
    @DisplayName("""
    Når event ikke er MediaStreamParsedEvent
    Hvis onEvent kalles
    Så:
        Kastes UnqualifiedEntryEventException
    """)
    fun testWrongEntryEventThrows() {
        assertThrowsExactly(
            SoftDispatchException.UnqualifiedEntryEventException::class.java
        ) {
            listener.onEvent(DummyEvent(), emptyList())
        }
    }

    @Test
    @DisplayName("""
    Når StartProcessingEvent mangler i historikken
    Hvis onEvent kalles
    Så:
        Returneres Rejected med riktig reason
    """)
    fun testMissingStartEvent() {
        val parsed = defaultMediaStreamParsedEvent().newReferenceId()

        val result = listener.onEvent(parsed, history)

        assertTrue(result is ValidateFileAndMediaDataEvent)
        val evt = result as ValidateFileAndMediaDataEvent
        assertEquals(ValidateFileAndMediaDataEvent.ValidationStatus.Rejected, evt.validationStatus)
        assertEquals("Missing StartProcessingEvent", evt.rejectionReason)
    }

    @Test
    @DisplayName("""
    Når CoordinatorReadStreamsResultEvent mangler
    Hvis onEvent kalles
    Så:
        Returneres Rejected med riktig reason
""")
    fun testMissingJsonEvent() {
        val start = StartProcessingEvent(
            StartData(operation = setOf(OperationType.Encode), fileUri = "/tmp/Test.mkv")
        )            .newReferenceId()
            .addToHistory()



        val parsed = defaultMediaStreamParsedEvent().derivedOf(start)


        val result = listener.onEvent(parsed, history)

        assertTrue(result is ValidateFileAndMediaDataEvent)
        val evt = result as ValidateFileAndMediaDataEvent
        assertEquals("Missing CoordinatorReadStreamsResultEvent", evt.rejectionReason)
    }

    @Test
    @DisplayName("""
    Når JSON-event finnes men format ikke kan parses
    Hvis onEvent kalles
    Så:
        Returneres Rejected med riktig reason
""")
    fun testUnparseableFormat() {
        val start = StartProcessingEvent(
            StartData(operation = setOf(OperationType.Encode), fileUri = "/tmp/file.mkv")
        ).newReferenceId().addToHistory()

        val json = CoordinatorReadStreamsResultEvent(
            status = TaskStatus.Completed,
            data = parse(
                """
            {
              "format": {
                "unexpected": true
              }
            }
            """.trimIndent()
            )
        ).derivedOf(start).addToHistory()

        val parsed = defaultMediaStreamParsedEvent().derivedOf(json).addToHistory()

        val result = listener.onEvent(parsed, history)

        assertTrue(result is ValidateFileAndMediaDataEvent)
        val evt = result as ValidateFileAndMediaDataEvent
        assertEquals("Failed to parse ffprobe format", evt.rejectionReason)
    }



    @Test
    @DisplayName("""
    Når functional validation feiler
    Hvis Encode krever video men ingen video finnes
    Så:
        Returneres Rejected
""")
    fun testFunctionalValidationFails() {
        val start = StartProcessingEvent(
            StartData(operation = setOf(OperationType.Encode), fileUri = "/tmp/file.mkv")
        ).newReferenceId()
            .addToHistory()

        val json = CoordinatorReadStreamsResultEvent(
            status = TaskStatus.Completed,
            data = parse(
                """
        {
          "format": {
            "nb_streams": 1,
            "nb_programs": 0,
            "format_name": "matroska,webm",
            "format_long_name": "Matroska / WebM",
            "start_time": "0.000000",
            "duration": "10.0",
            "size": "1000",
            "bit_rate": "1000",
            "probe_score": 100
          }
        }
        """.trimIndent()
            )
        ).derivedOf(start).addToHistory()


        val parsed = MediaStreamParsedEvent(
            ParsedMediaStreams(audioStream = listOf(dummyAudioStream(0)))
        ).derivedOf(json)

        val result = listener.onEvent(parsed, history)

        assertTrue(result is ValidateFileAndMediaDataEvent)
        val evt = result as ValidateFileAndMediaDataEvent
        assertEquals("Encode requires at least one video stream", evt.rejectionReason)
    }


    @Test
    @DisplayName("""
    Når alle valideringer passerer
    Hvis onEvent kalles
    Så:
        Returneres Ok med eventuelle warnings
""")
    fun testValidationOk() {
        IFile.factory = { path ->
            when (path) {
                "build/test-intermediate/okfile.mkv" ->
                    FakeFile(path, exists = true, size = 1000)
                else ->
                    FakeFile(path, exists = false, size = 0)
            }
        }


        val start = StartProcessingEvent(
            StartData(operation = setOf(OperationType.Encode), fileUri = "build/test-intermediate/okfile.mkv")
        ).newReferenceId()
            .addToHistory()

        val json = CoordinatorReadStreamsResultEvent(
            status = TaskStatus.Completed,
            data = parse("""{ "format": { 
            "duration": "10.0",
            "size": "1000",
            "bit_rate": "1000",
            "nb_streams": 3,
            "probe_score": 100
        }}""")
        )
            .derivedOf(start)
            .addToHistory()

        val parsed = MediaStreamParsedEvent(
            ParsedMediaStreams(
                videoStream = listOf(dummyVideoStream(0).copy(duration = "10.0")),
                audioStream = listOf(dummyAudioStream(1).copy(duration = "10.0")),
                subtitleStream = emptyList()
            )
        ).derivedOf(json)

        val result = listener.onEvent(parsed, history)

        assertTrue(result is ValidateFileAndMediaDataEvent)
        val evt = result as ValidateFileAndMediaDataEvent

        assertEquals(ValidateFileAndMediaDataEvent.ValidationStatus.Ok, evt.validationStatus)
        assertEquals(ValidateFileAndMediaDataEvent.ValidationSeverity.None, evt.severity)
    }

    @Test
    @DisplayName("""
    Når ffprobe-format mangler ett av minimumskravene
    Hvis onEvent kalles
    Så:
        Returneres "Failed to parse ffprobe format"
""")
    fun testMinimumRequiredFieldsForFormat() {
        val requiredFields = listOf("duration", "size", "bit_rate", "nb_streams", "probe_score")

        requiredFields.forEach { missingField ->

            val start = StartProcessingEvent(
                StartData(operation = emptySet(), fileUri = "/tmp/file.mkv")
            ).newReferenceId().addToHistory()

            // Bygg JSON med alle felter, fjern ett
            val fullJson = mutableMapOf(
                "duration" to "10.0",
                "size" to "1000",
                "bit_rate" to "1000",
                "nb_streams" to 1,
                "probe_score" to 100
            )

            fullJson.remove(missingField)

            val json = CoordinatorReadStreamsResultEvent(
                status = TaskStatus.Completed,
                data = parse(
                    """
                {
                  "format": ${Gson().toJson(fullJson)}
                }
                """.trimIndent()
                )
            ).derivedOf(start).addToHistory()

            val parsed = MediaStreamParsedEvent(
                ParsedMediaStreams(
                    videoStream = emptyList(),
                    audioStream = emptyList(),
                    subtitleStream = emptyList()
                )
            ).derivedOf(json).addToHistory()

            val result = listener.onEvent(parsed, history)

            assertTrue(result is ValidateFileAndMediaDataEvent, "Expected event for missing $missingField")
            val evt = result as ValidateFileAndMediaDataEvent
            assertEquals(
                "Failed to parse ffprobe format",
                evt.rejectionReason,
                "Missing field '$missingField' should cause parse failure"
            )
        }
    }



}