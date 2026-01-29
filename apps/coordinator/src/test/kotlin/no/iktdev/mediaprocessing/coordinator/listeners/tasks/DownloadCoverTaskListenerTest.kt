package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.MockDownloadClient
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.shared.common.DownloadClient
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.CoverDownloadTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis

class DownloadCoverTaskListenerTest: TestBase() {

    class DownloadCoverTaskListenerTestImplementation(coordinatorEnv: CoordinatorEnv) : DownloadCoverTaskListener(coordinatorEnv) {
        fun getJob() = currentJob

        lateinit var client: DownloadClient
        override fun getDownloadClient(): DownloadClient = client

        private var _result: Event? = null
        fun getResult(): Event? = _result

        override fun onComplete(task: Task, result: Event?) {
            super.onComplete(task, result)
            this._result = result
        }
    }

    private val overrideReporter = object : TaskReporter {
        override fun markClaimed(taskId: UUID, workerId: String) {}
        override fun updateLastSeen(taskId: UUID) {}
        override fun markCompleted(taskId: UUID) {}
        override fun markFailed(taskId: UUID) {}
        override fun updateProgress(taskId: UUID, progress: Int) {}
        override fun log(taskId: UUID, message: String) {}
        override fun publishEvent(event: Event) {}
    }

    private var listener = DownloadCoverTaskListenerTestImplementation(coordinatorEnv)

    @Test
    @DisplayName(
        """
        Når onTask kjøres
        Hvis nedlasting tar tid
        Så:
            venter listener til jobben er ferdig og returnerer Completed-event
        """
    )
    fun onTask_waits_for_runner_to_complete() = runTest {
        val delay = 1000L

        val task = CoverDownloadTask(
            CoverDownloadTask.CoverDownloadData(
                url = "http://example.com/fancy.jpg",
                outputFileName = "potatoland",
                source = "fancy"
            )
        ).newReferenceId()

        listener = DownloadCoverTaskListenerTestImplementation(coordinatorEnv).apply {
            this.client = MockDownloadClient(
                delayMillis = delay,
                mockFile = File("/tmp/fancy.jpg")
            )
        }

        val time = measureTimeMillis {
            listener.accept(task, overrideReporter)
            listener.getJob()?.join()

            val event = listener.getResult()
            assertTrue(event is CoverDownloadResultEvent)
            assertEquals(TaskStatus.Completed, (event as CoverDownloadResultEvent).status)
        }

        assertTrue(time >= delay, "Expected at least $delay ms, got $time ms")
        assertTrue(time <= delay * 2, "Expected less than ${delay * 2} ms, got $time ms")
    }

    @Test
    @DisplayName(
        """
        Når onTask kjøres
        Hvis download-klienten kaster en exception
        Så:
            returneres Failed-event
        """
    )
    fun onTask_returns_failed_on_exception() = runTest {
        val task = CoverDownloadTask(
            CoverDownloadTask.CoverDownloadData(
                url = "http://example.com/fancy.jpg",
                outputFileName = "potatoland",
                source = "fancy"
            )
        ).newReferenceId()

        listener = DownloadCoverTaskListenerTestImplementation(coordinatorEnv).apply {
            this.client = MockDownloadClient(throwException = true)
        }

        listener.accept(task, overrideReporter)
        listener.getJob()?.join()

        val event = listener.getResult()
        assertTrue(event is CoverDownloadResultEvent)
        assertEquals(TaskStatus.Failed, (event as CoverDownloadResultEvent).status)
    }

    @Test
    @DisplayName(
        """
        Når onTask kjøres
        Hvis task ikke er av typen CoverDownloadTask
        Så:
            returneres null
        """
    )
    fun onTask_returns_null_for_unsupported_task() = runTest {
        val event = listener.onTask(TestBase.DummyTask()) // fake unsupported task
        assertNull(event)
    }

    @Test
    @DisplayName(
        """
        Når onTask produserer event
        Hvis nedlasting lykkes
        Så:
            inneholder event korrekt filsti
        """
    )
    fun onTask_produces_correct_output_path() = runTest {
        val mockFile = File("/tmp/expected.jpg")

        listener = DownloadCoverTaskListenerTestImplementation(coordinatorEnv).apply {
            this.client = MockDownloadClient(mockFile = mockFile)
        }

        val task = CoverDownloadTask(
            CoverDownloadTask.CoverDownloadData(
                url = "http://example.com/img.jpg",
                outputFileName = "expected",
                source = "unit-test"
            )
        ).newReferenceId()

        listener.accept(task, overrideReporter)
        listener.getJob()?.join()

        val event = listener.getResult() as CoverDownloadResultEvent
        assertEquals(mockFile.absolutePath, event.data!!.outputFile)
    }

    @Test
    @DisplayName(
        """
        Når accept kalles
        Hvis nedlasting skjer asynkront
        Så:
            blokkerer ikke tråden
        """
    )
    fun accept_is_non_blocking() = runTest {
        val delay = 500L

        listener = DownloadCoverTaskListenerTestImplementation(coordinatorEnv).apply {
            this.client = MockDownloadClient(delayMillis = delay, mockFile = File("/tmp/x.jpg"))
        }

        val task = CoverDownloadTask(
            CoverDownloadTask.CoverDownloadData(
                url = "http://example.com/img.jpg",
                outputFileName = "x",
                source = "unit-test"
            )
        ).newReferenceId()

        val time = measureTimeMillis {
            listener.accept(task, overrideReporter)
            // intentionally NOT joining here
        }

        assertTrue(time < 50, "accept() should return immediately, got $time ms")
    }
}
