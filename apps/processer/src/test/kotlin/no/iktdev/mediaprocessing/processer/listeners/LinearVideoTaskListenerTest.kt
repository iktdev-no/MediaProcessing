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
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.TestUtils
import no.iktdev.mediaprocessing.processer.assertSameReferenceId
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.processer.getCoordinatorClient
import no.iktdev.mediaprocessing.processer.getProcesserProperties
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import no.iktdev.mediaprocessing.shared.common.model.task.data.LinearEncodeData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.system.measureTimeMillis

class LinearVideoTaskListenerTest : TestBase() {


    class TestListenerLinear(
        val delay: Long,
        coordinatorClient: CoordinatorClient,
        processerProperties: ProcesserProperties,
        executablesConfig: ExecutablesConfig
    ) :
        LinearVideoTaskListener(
            coordinatorWebClient = coordinatorClient,
            localProgress = LocalProgressCache(),
            fileUtil = TestUtils.getFileUtil(),
            executableConfig = executablesConfig,
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

        override fun buildFfmpeg(listener: FFmpeg.Listener?, execPath: String, logDirectory: IFile): FFmpeg {
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
        TaskTypeRegistry.register(LinearEncodeTask::class.java)
    }


    @Test
    fun `onTask waits for runner to complete`() = runTest {
        val delay = 1000L
        val testTask = LinearEncodeTask(
            LinearEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {} },
                    output = OutputSection("output.mp4").apply {
                        overwrite = true
                    },
                ),
            )
        ).newReferenceId()

        val listener = TestListenerLinear(delay, getCoordinatorClient(), getProcesserProperties(), mockExecConfig)

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
    @DisplayName(
        """
        Når en event produseres fra en task
        Hvis task har en gitt referenceId
        Så:
            Skal eventen ha samme referenceId
        """
    )
    fun producedFrom_keeps_referenceId() {
        val task = LinearEncodeTask(
            LinearEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {} },
                    output = OutputSection("output.mp4").apply {
                        overwrite = true
                    },
                ),
            )
        ).newReferenceId()

        val event = ProcesserEncodeResultEvent(
            status = TaskStatus.Completed
        ).producedFrom(task)

        assertSameReferenceId(task, event)
    }

    @Test
    @DisplayName(
        """
        Når en task feiler og createIncompleteStateTaskEvent kalles
        Hvis task har en referenceId
        Så:
            Skal eventen som returneres ha samme referenceId
    """
    )
    fun createIncompleteStateTaskEvent_keeps_referenceId() {
        val task = LinearEncodeTask(
            LinearEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {} },
                    output = OutputSection("output.mp4").apply {
                        overwrite = true
                    },
                ),
            )
        ).newReferenceId()

        val listener = LinearVideoTaskListener(
            coordinatorWebClient = getCoordinatorClient(),
            localProgress = LocalProgressCache(),
            fileUtil = TestUtils.getFileUtil(),
            executableConfig = mockExecConfig,
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
    @DisplayName(
        """
        Når VideoTaskListener kjører en EncodeTask
        Hvis task har en referenceId
        Så:
            Skal resultat-eventen ha samme referenceId
        """
    )
    fun onTask_keeps_referenceId() = runTest {
        val task = LinearEncodeTask(
            LinearEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {} },
                    output = OutputSection("output.mp4").apply {
                        overwrite = true
                    },
                ),
            )
        ).apply { newReferenceId() }

        val listener = TestListenerLinear(delay = 10, getCoordinatorClient(), getProcesserProperties(), mockExecConfig)

        listener.accept(task, overrideReporter)
        listener.getJob()?.join()

        val event = listener.getResult()
        assertTrue(event is ProcesserEncodeResultEvent)
        assertSameReferenceId(task, event)
    }


}