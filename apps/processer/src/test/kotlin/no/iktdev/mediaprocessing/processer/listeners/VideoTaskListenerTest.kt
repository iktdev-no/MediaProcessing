package no.iktdev.mediaprocessing.processer.listeners

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskTypeRegistry
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.TestUtils
import no.iktdev.mediaprocessing.processer.assertSameReferenceId
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis

class VideoTaskListenerTest {


    class TestListener(val delay: Long, coordinatorClient: CoordinatorClient) :
        VideoTaskListener(
            coordinatorWebClient = coordinatorClient,
            localProgress = LocalProgressCache(),
            fileUtil = TestUtils.getFileUtil(),
            executableConfig = TestUtils.getExecutableConfig(),
        ) {
        fun getJob() = currentJob

        private var _result: Event? = null
        fun getResult(): Event? {
            return _result
        }

        override fun onComplete(task: Task, result: Event?) {
            this._result = result
        }

        override fun buildFfmpeg(listener: FFmpeg.Listener?, execPath: String, logDirectory: File): FFmpeg {
            return MockFFmpeg(delayMillis = delay, listener = MockFFmpeg.emptyListener())
        }
    }

    val overrideReporter = object : TaskReporter {
        override fun markClaimed(taskId: UUID, workerId: String) {}
        override fun updateLastSeen(taskId: UUID) {}
        override fun markCompleted(taskId: UUID) {}
        override fun markFailed(referenceId: UUID, taskId: UUID) {}
        override fun markCancelled(referenceId: UUID, taskId: UUID) {}
        override fun updateProgress(taskId: UUID, progress: Int) {}
        override fun log(taskId: UUID, message: String) {}
        override fun publishEvent(event: Event) {

        }
    }

    @BeforeEach
    fun setup() {
        TaskTypeRegistry.register(EncodeTask::class.java)
    }

    private val coordinatorClient = mockk<CoordinatorClient>(relaxed = true)

    @Test
    fun `onTask waits for runner to complete`() = runTest {
        val delay = 1000L
        val testTask = EncodeTask(
            EncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                arguments = listOf("-y")
            )
        ).newReferenceId()

        val listener = TestListener(delay, coordinatorClient)

        val time = measureTimeMillis {
            listener.accept(testTask, overrideReporter)
            listener.getJob()?.join()
            val event = listener.getResult()
            assertTrue(event is ProcesserEncodeResultEvent)
            assertEquals(TaskStatus.Completed, (event as ProcesserEncodeResultEvent).status)
        }

        assertTrue(time >= delay, "Expected onTask to wait at least $delay ms, waited for $time ms")
        assertTrue(time <= (delay * 2), "Expected onTask to wait less than ${(delay * 2)} ms, waited for $time ms")

    }

    @Test
    @DisplayName("""
        Når en event produseres fra en task
        Hvis task har en gitt referenceId
        Så:
            Skal eventen ha samme referenceId
        """
    )
    fun producedFrom_keeps_referenceId() {
        val task = EncodeTask(
            EncodeData(inputFile = "input.mp4", outputFileName = "output.mp4", arguments = listOf("-y"))
        ).newReferenceId()

        val event = ProcesserEncodeResultEvent(
            status = TaskStatus.Completed
        ).producedFrom(task)

        assertSameReferenceId(task, event)
    }

    @Test
    @DisplayName("""
        Når en task feiler og createIncompleteStateTaskEvent kalles
        Hvis task har en referenceId
        Så:
            Skal eventen som returneres ha samme referenceId
    """)
    fun createIncompleteStateTaskEvent_keeps_referenceId() {
        val task = EncodeTask(
            EncodeData(inputFile = "input.mp4", outputFileName = "output.mp4", arguments = listOf("-y"))
        ).newReferenceId()

        val listener = VideoTaskListener(
            coordinatorWebClient = coordinatorClient,
            localProgress = LocalProgressCache(),
            fileUtil = TestUtils.getFileUtil(),
            executableConfig = TestUtils.getExecutableConfig()
        )

        val event = listener.createIncompleteStateTaskEvent(
            task = task,
            status = TaskStatus.Failed,
            exception = RuntimeException("boom")
        )

        assertSameReferenceId(task, event)
    }

    @Test
    @DisplayName("""
        Når VideoTaskListener kjører en EncodeTask
        Hvis task har en referenceId
        Så:
            Skal resultat-eventen ha samme referenceId
        """)
    fun onTask_keeps_referenceId() = runTest {
        val task = EncodeTask(
            EncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                arguments = listOf("-y")
            )
        ).newReferenceId()

        val listener = TestListener(delay = 10, coordinatorClient)

        listener.accept(task, overrideReporter)
        listener.getJob()?.join()

        val event = listener.getResult()
        assertTrue(event is ProcesserEncodeResultEvent)
        assertSameReferenceId(task, event)
    }




}