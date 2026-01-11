package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import com.google.gson.JsonObject
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.MockFFprobe
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.ffmpeg.FFprobe
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoordinatorReadStreamsResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MediaReadTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.system.measureTimeMillis

class MediaStreamReadTaskListenerTest: TestBase() {

    class MediaStreamReadTaskListenerTestImplementation(coordinatorEnv: CoordinatorEnv): MediaStreamReadTaskListener(coordinatorEnv) {
        fun getJob() = currentJob

        lateinit var probe: FFprobe
        override fun getFfprobe(): FFprobe {
            return probe
        }

        private var _result: Event? = null
        fun getResult(): Event? {
            return _result
        }
        override fun onComplete(task: Task, result: Event?) {
            super.onComplete(task, result)
            this._result = result
        }
    }

    val overrideReporter = object : TaskReporter {
        override fun markClaimed(taskId: UUID, workerId: String) {}
        override fun updateLastSeen(taskId: UUID) {}
        override fun markConsumed(taskId: UUID) {}
        override fun updateProgress(taskId: UUID, progress: Int) {}
        override fun log(taskId: UUID, message: String) {}
        override fun publishEvent(event: Event) {

        }
    }

    var listener = MediaStreamReadTaskListenerTestImplementation(coordinatorEnv)

    @BeforeEach
    fun resetListener() {
        listener = MediaStreamReadTaskListenerTestImplementation(coordinatorEnv)
    }


    @Test
    fun `onTask waits for runner to complete`() = runTest {
        val delay = 1000L

        val json = JsonObject().apply { addProperty("codec_type", "video") }

        val task = MediaReadTask(fileUri = "test.mp4").newReferenceId()

        listener = MediaStreamReadTaskListenerTestImplementation(coordinatorEnv).apply {
            this.probe = MockFFprobe.success(json, delay)
        }


        val time = measureTimeMillis {
            listener.accept(task, overrideReporter)
            listener.getJob()?.join()
            val event = listener.getResult()
            assertTrue(event is CoordinatorReadStreamsResultEvent)
            val result = event as CoordinatorReadStreamsResultEvent
            assertEquals(json, result.data)
            assertEquals(TaskStatus.Completed, result.status)
            assertEquals("test.mp4", (listener.probe as MockFFprobe).lastInputFile)
            assertEquals(TaskStatus.Completed, (event as CoordinatorReadStreamsResultEvent).status)
        }

        assertTrue(time >= delay, "Expected onTask to wait at least $delay ms, waited for $time ms")
        assertTrue(time <= (delay*2), "Expected onTask to wait less than ${(delay*2)} ms, waited for $time ms")

    }


    @Test
    @DisplayName(
        "Når støtter sjekk for MediaReadTask" +
                "Hvis task er av typen MediaReadTask" +
                "Så:" +
                "    returnerer true"
    )
    fun `supports returns true for MediaReadTask`() {
        val mediaTask = mockk<MediaReadTask>()
        assertTrue(listener.supports(mediaTask))
    }

    @Test
    @DisplayName(
        "Når støtter sjekk for ikke-MediaReadTask" +
                "Hvis task ikke er av typen MediaReadTask" +
                "Så:" +
                "    returnerer false"
    )
    fun `supports returns false for non MediaReadTask`() {
        val otherTask = mockk<Task>()
        assertFalse(listener.supports(otherTask))
    }

    @Test
    @DisplayName(
        "Når onTask kalles med ikke-MediaReadTask" +
                "Hvis task ikke kan castes til MediaReadTask" +
                "Så:" +
                "    returnerer null"
    )
    fun `onTask returns null for non MediaReadTask`() = runBlocking {
        val otherTask = mockk<Task>()
        val result = listener.onTask(otherTask)
        assertNull(result)
    }

    @Test
    @DisplayName(
        "Når genererer worker id" +
                "Hvis worker id blir forespurt" +
                "Så:" +
                "    inneholder id klasse navn og task type"
    )
    fun `getWorkerId contains class name and task type`() {
        val id = listener.getWorkerId()
        assertTrue(id.contains("MediaStreamReadTaskListener"))
        assertTrue(id.contains("CPU_INTENSIVE"))
    }

    @Test
    @DisplayName("""
        Når en MediaReadTask med gyldig filUri prosesseres
        Hvis FFprobe returnerer et gyldig JSON-objekt
        Så:
            Skal MediaStreamReadEvent produseres med data
    """)
    fun verifyEventProducedOnValidJson() = runTest {
        val json = JsonObject().apply { addProperty("codec_type", "video") }
        listener.probe = MockFFprobe.success(json)

        val task = MediaReadTask(fileUri = "test.mp4").newReferenceId()
        val event = listener.onTask(task)

        assertNotNull(event)
        assertTrue(event is CoordinatorReadStreamsResultEvent)
        val result = event as CoordinatorReadStreamsResultEvent
        assertEquals(json, result.data)
        assertEquals(TaskStatus.Completed, result.status)
        assertEquals("test.mp4", (listener.probe as MockFFprobe).lastInputFile)
    }

    @Test
    @DisplayName("""
        Når en MediaReadTask med ugyldig filUri prosesseres
        Hvis FFprobe feiler med parsing
        Så:
            Skal onTask returnere null og ikke kaste unntak
    """)
    fun verifyNullOnParsingError() = runTest {
        listener.probe = MockFFprobe.failure("Could not parse")

        val task = MediaReadTask(fileUri = "corrupt.mp4").newReferenceId()
        val event = listener.onTask(task)
        val result = event as CoordinatorReadStreamsResultEvent

        assertNull(result.data)
        assertEquals(TaskStatus.Failed, result.status)
        assertEquals("corrupt.mp4", (listener.probe as MockFFprobe).lastInputFile)
    }

    @Test
    @DisplayName("""
        Når en MediaReadTask prosesseres
        Hvis FFprobe kaster exception
        Så:
            Skal onTask returnere null og logge feilen
    """)
    fun verifyExceptionHandling() = runTest {
        listener.probe = MockFFprobe.exception()

        val task = MediaReadTask(fileUri = "broken.mp4").newReferenceId()
        val event = listener.onTask(task)
        assertInstanceOf(CoordinatorReadStreamsResultEvent::class.java, event)
        val resultEvent = event as CoordinatorReadStreamsResultEvent
        assertNull(event.data)
        assertEquals(TaskStatus.Failed, event.status)
    }

    @Test
    @DisplayName("""
        Når en Task som ikke er MediaReadTask prosesseres
        Hvis supports sjekkes
        Så:
            Skal supports returnere false og onTask returnere null
    """)
    fun verifySupportsOnlyMediaReadTask() = runTest {
        listener.probe = MockFFprobe.failure("Not used")

        val otherTask = object : Task() {}.newReferenceId()
        assertFalse(listener.supports(otherTask))
        val event = listener.onTask(otherTask)
        assertNull(event)
    }
}