package no.iktdev.mediaprocessing.processer.listeners

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.registry.TaskTypeRegistry
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.CoordinatorClient
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.processer.TestUtils
import no.iktdev.mediaprocessing.processer.assertSameReferenceId
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.processer.getCoordinatorClient
import no.iktdev.mediaprocessing.processer.getProcesserProperties
import no.iktdev.mediaprocessing.processer.strategy.VideoStrategy
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

class LinearVideoTaskListenerTest {


    class TestListenerLinear(val delay: Long, coordinatorClient: CoordinatorClient, processerProperties: ProcesserProperties) :
        LinearVideoTaskListener(
            coordinatorWebClient = coordinatorClient,
            localProgress = LocalProgressCache(),
            fileUtil = TestUtils.getFileUtil(),
            executableConfig = TestUtils.getExecutableConfig(),
            processerProperties = processerProperties
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
        override fun markClaimed(taskId: UUID, workerId: String): Result { return Result.Success }
        override fun updateLastSeen(taskId: UUID): Result { return Result.Success }
        override fun markCompleted(taskId: UUID): Result { return Result.Success }
        override fun markFailed(referenceId: UUID, taskId: UUID): Result { return Result.Success }
        override fun markCancelled(referenceId: UUID, taskId: UUID): Result { return Result.Success }
        override fun updateProgress(referenceId: UUID, taskId: UUID, payload: Progress): Result { return Result.Success }
        override fun log(taskId: UUID, message: String) {}
        override fun publishEvent(event: Event): Result {
            return Result.Success
        }
    }

    @BeforeEach
    fun setup() {
        TaskTypeRegistry.register(EncodeTask::class.java)
    }


    @Test
    fun `onTask waits for runner to complete`() = runTest {
        val delay = 1000L
        val testTask = EncodeTask(
            EncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                arguments = listOf("-y")
            )
        ).newReferenceId()

        val listener = TestListenerLinear(delay, getCoordinatorClient(), getProcesserProperties())

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
            EncodeData(inputFile = "input.mp4", outputFileName = "output.mp4", outputFolderName = "output", arguments = listOf("-y"))
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
            EncodeData(inputFile = "input.mp4", outputFileName = "output.mp4", outputFolderName = "output", arguments = listOf("-y"))
        ).newReferenceId()

        val listener = LinearVideoTaskListener(
            coordinatorWebClient = getCoordinatorClient(),
            localProgress = LocalProgressCache(),
            fileUtil = TestUtils.getFileUtil(),
            executableConfig = TestUtils.getExecutableConfig(),
            processerProperties = getProcesserProperties()
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
                outputFolderName = "output",
                arguments = listOf("-y")
            )
        ).apply { newReferenceId() }

        val listener = TestListenerLinear(delay = 10, getCoordinatorClient(), getProcesserProperties())

        listener.accept(task, overrideReporter)
        listener.getJob()?.join()

        val event = listener.getResult()
        assertTrue(event is ProcesserEncodeResultEvent)
        assertSameReferenceId(task, event)
    }

    @Test
    @DisplayName("""
    Når encode-argumentene kun påvirker audio (f.eks. -c:a aac)
    Hvis getEncodeStrategy() kalles
    Så:
        Skal Linear returneres
""")
    fun strategy_returns_linear_for_audio_only() {
        val listener = TestListenerLinear(delay = 0, getCoordinatorClient(), getProcesserProperties())

        val task = EncodeTask(
            EncodeData(
                inputFile = "input.mp4",
                outputFileName = "out.mp4",
                outputFolderName = "output",
                arguments = listOf("-c:a", "aac")
            )
        ).apply { newReferenceId() }

        val strategy = listener.getEncodeStrategy(task)

        assertEquals(VideoStrategy.Linear, strategy)
    }

    @Test
    @DisplayName("""
    Når encode-strategien er Linear
    Hvis accept() kalles på LinearVideoTaskListener
    Så:
        Skal listeneren akseptere tasken
""")
    fun linear_listener_accepts_when_strategy_is_linear() {
        val listener = TestListenerLinear(delay = 0, getCoordinatorClient(), getProcesserProperties())

        val task = EncodeTask(
            EncodeData(
                inputFile = "input.mp4",
                outputFileName = "out.mp4",
                outputFolderName = "output",
                arguments = listOf("-c:a", "aac") // audio-only → linear
            )
        ).apply { newReferenceId() }

        val accepted = listener.accept(task, overrideReporter)

        assertTrue(accepted)
    }

    @Test
    @DisplayName("""
    Når encode-strategien er Segmented
    Hvis accept() kalles på LinearVideoTaskListener
    Så:
        Skal listeneren IKKE akseptere tasken
""")
    fun linear_listener_rejects_when_strategy_is_segmented() {
        val listener = TestListenerLinear(delay = 0, getCoordinatorClient(), getProcesserProperties())

        val task = EncodeTask(
            EncodeData(
                inputFile = "input.mp4",
                outputFileName = "out.mp4",
                outputFolderName = "output",
                arguments = listOf("-c:v", "libx264") // video → segmented
            )
        ).apply { newReferenceId() }

        val accepted = listener.accept(task, overrideReporter)

        assertTrue(!accepted)
    }



}