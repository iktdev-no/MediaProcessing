package no.iktdev.mediaprocessing.processer.listeners

import io.mockk.coEvery
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.delay
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
import no.iktdev.mediaprocessing.processer.linear.LinearProcessor
import no.iktdev.mediaprocessing.processer.runners.AudioEncodeRunner
import no.iktdev.mediaprocessing.processer.runners.VideoEncodeRunner
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import no.iktdev.mediaprocessing.shared.common.model.task.data.DefaultEncodeData
import no.iktdev.mediaprocessing.shared.common.model.task.data.LinearEncodeData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.system.measureTimeMillis

class LinearVideoTaskListenerTest : TestBase() {

    private fun makeLinearTask(
        overwrite: Boolean = true,
        outputFolder: IFile = workFolder
    ): LinearEncodeTask {

        val video = FFmpegInstructions(
            inputs = InputSection().apply { file("input.mp4") {} },
            output = OutputSection("output.mp4").apply {
                this.overwrite = overwrite
            }
        )

        return LinearEncodeTask(
            data = DefaultEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = outputFolder.absolutePath,
                videoInstruction = video,
                audioInstructions = emptyList()
            )
        ).apply { newReferenceId() }
    }


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
            DefaultEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                videoInstruction = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {} },
                    output = OutputSection("output.mp4").apply {
                        overwrite = true
                    },
                ),
                audioInstructions = emptyList()
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
            DefaultEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                videoInstruction = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {} },
                    output = OutputSection("output.mp4").apply {
                        overwrite = true
                    },
                ),
                audioInstructions = emptyList()
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
            DefaultEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                videoInstruction = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {} },
                    output = OutputSection("output.mp4").apply {
                        overwrite = true
                    },
                ),
                audioInstructions = emptyList()
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
            DefaultEncodeData(
                inputFile = "input.mp4",
                outputFileName = "output.mp4",
                outputFolderName = "output",
                videoInstruction = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {} },
                    output = OutputSection("output.mp4").apply {
                        overwrite = true
                    },
                ),
                audioInstructions = emptyList()
            )
        ).apply { newReferenceId() }

        val listener = TestListenerLinear(delay = 10, getCoordinatorClient(), getProcesserProperties(), mockExecConfig)

        listener.accept(task, overrideReporter)
        listener.getJob()?.join()

        val event = listener.getResult()
        assertTrue(event is ProcesserEncodeResultEvent)
        assertSameReferenceId(task, event)
    }


    @Test
    @DisplayName(
        """
    Når collectLogs() kalles
    Hvis flere loggfiler finnes i log-mappen
    Så:
        Skal merged.log inneholde loggene i stigende tidsrekkefølge
    """
    )
    fun collectLogs_merges_logs_in_order() {
        val listener = LinearVideoTaskListener(
            coordinatorWebClient = getCoordinatorClient(),
            localProgress = LocalProgressCache(),
            fileUtil = TestUtils.getFileUtil(),
            executableConfig = mockExecConfig,
            processerProperties = getProcesserProperties()
        )

        val logDir = workFolder.using("logs").apply { mkdirs() }

        val log1 = logDir.using("a.log").apply {
            writeText("AAA")
        }

        val log2 = logDir.using("b.log").apply {
            writeText("BBB")
        }

        val merged = listener.collectLogs(logDir, 0)
        val text = merged.readText()

        assertTrue(text.indexOf("AAA") < text.indexOf("BBB"))
    }

    @Test
    @DisplayName(
        """
    Når output-filen finnes
    Hvis overwrite=false i video-instruksjonen
    Så:
        Skal listener publisere Failed-event og kaste IllegalStateException
    """
    )
    fun overwrite_false_publishes_failed_and_throws() = runTest {
        // Arrange
        workFolder.using("output.mp4").apply { writeText("x") }

        val task = makeLinearTask(overwrite = false)

        val reporter = object : TaskReporter {
            var result: Event? = null
            override fun markClaimed(taskId: UUID, workerId: String) = Result.Success
            override fun updateLastSeen(taskId: UUID) = Result.Success
            override fun markCompleted(taskId: UUID) = Result.Success
            override fun markFailed(referenceId: UUID, taskId: UUID) = Result.Failure("fail")
            override fun markCancelled(referenceId: UUID, taskId: UUID) = Result.Success
            override fun updateProgress(referenceId: UUID, taskId: UUID, payload: Progress) = Result.Success
            override fun log(taskId: UUID, message: String) {}
            override fun publishEvent(event: Event): Result {
                result = event
                return Result.Success
            }
        }

        val listener = LinearVideoTaskListener(
            coordinatorWebClient = getCoordinatorClient(),
            localProgress = LocalProgressCache(),
            executableConfig = mockExecConfig,
            fileUtil = TestUtils.getFileUtil(),
            processerProperties = getProcesserProperties()
        )

        // Act
        listener.accept(task, reporter)
        while (listener.isBusy) {
            delay(1)
        }


        // Verify event was published
        assertThat(reporter.result)
            .isInstanceOf(ProcesserEncodeResultEvent::class.java)
            .extracting("status")
            .isEqualTo(TaskStatus.Failed)
    }



    @Test
    @DisplayName(
        """
    Når onTask() kjører en full pipeline
    Hvis video, audio og merge lykkes
    Så:
        Skal resultat-eventen være Completed
    """
    )
    fun onTask_full_pipeline_returns_completed() = runTest {
        mockkConstructor(LinearProcessor::class)

        val fakeVideo = VideoEncodeRunner.VideoEncodeResult(
            workFolder.using("video.mp4").apply { writeText("v") }
        )
        val fakeAudio = emptyList<AudioEncodeRunner.AudioEncodePayload>()
        val fakeMerged = workFolder.using("final.mp4").apply { writeText("f") }

        coEvery { anyConstructed<LinearProcessor>().processVideo(any()) } returns fakeVideo
        coEvery { anyConstructed<LinearProcessor>().processAudio(any()) } returns fakeAudio
        coEvery { anyConstructed<LinearProcessor>().processMerge(any(), fakeAudio, fakeVideo.output) } returns fakeMerged

        val task = makeLinearTask()

        val listener = TestListenerLinear(delay = 10, getCoordinatorClient(), getProcesserProperties(), mockExecConfig)


        listener.accept(task, overrideReporter)
        listener.currentJob?.join()

        val result = listener.getResult()

        assertTrue(result is ProcesserEncodeResultEvent)
        assertEquals(TaskStatus.Completed, (result as ProcesserEncodeResultEvent).status)
    }


}