package no.iktdev.mediaprocessing.processer.listeners

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.registry.TaskTypeRegistry
import no.iktdev.eventi.serialization.WGson
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.processer.TestBase
import no.iktdev.mediaprocessing.processer.TestUtils
import no.iktdev.mediaprocessing.processer.assertSameReferenceId
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*
import kotlin.system.measureTimeMillis

class SubtitleTaskListenerTest: TestBase() {

    class TestListener(val delay: Long, executablesConfig: ExecutablesConfig): SubtitleTaskListener(
        fileUtil = TestUtils.getFileUtil(),
        executableConfig = executablesConfig,
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
            return MockFFmpeg(delayMillis = delay, listener =  MockFFmpeg.emptyListener())
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
        TaskTypeRegistry.register(ExtractSubtitleTask::class.java)
    }

    @Test
    fun `onTask waits for runner to complete`() = runTest {
        val delay = 1000L
        val testTask = ExtractSubtitleTask(
            ExtractSubtitleData(
                inputFile = "input.mp4",
                outputFileName = "output.srt",
                outputFolderName = "output",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply { file("input.mp4") {
                        subtitle(0) {
                            map = true
                            language = "eng"
                        }
                    } },
                    output = OutputSection("output.srt").apply {
                        overwrite = true
                    },
                ),
                language = "eng"
            )
        ).newReferenceId()

        val listener = TestListener(delay, mockExecConfig)

        val time = measureTimeMillis {
            val accepted = listener.accept(testTask, overrideReporter)
            assertTrue(accepted, "Task listener did not accept the task.")
            listener.getJob()?.join()
            val event = listener.getResult()
            assertTrue(event is ProcesserExtractResultEvent)
            assertEquals(TaskStatus.Completed, (event as ProcesserExtractResultEvent).status)
        }

        assertTrue(time >= delay, "Expected onTask to wait at least $delay ms, waited for $time ms")
        assertTrue(time <= (delay*2), "Expected onTask to wait less than ${(delay*2)} ms, waited for $time ms")

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
        val task = ExtractSubtitleTask(
            ExtractSubtitleData(
                inputFile = "input.mp4",
                outputFileName = "output.srt",
                outputFolderName = "output",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply {
                        file("input.mp4") {
                            subtitle(0) {
                                map = true
                                language = "eng"
                            }
                        }
                    },
                    output = OutputSection("output.srt").apply {
                        overwrite = true
                    }
                ),

                language = "eng"
            )
        ).newReferenceId()

        val event = ProcesserExtractResultEvent(
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
        val task = ExtractSubtitleTask(
            ExtractSubtitleData(
                inputFile = "input.mp4",
                outputFolderName = "output",
                outputFileName = "output.srt",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply {
                        file("input.mp4") {
                            subtitle(0) {
                                map = true
                                language = "eng"
                            }
                        }
                    },
                    output = OutputSection("output.srt").apply {
                        overwrite = true
                    }
                ),

                language = "eng"
            )
        ).newReferenceId()

        val listener = SubtitleTaskListener(
            fileUtil = TestUtils.getFileUtil(),
            executableConfig = mockExecConfig
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
        val task = ExtractSubtitleTask(
            ExtractSubtitleData(
                inputFile = "input.mp4",
                outputFolderName = "output",
                outputFileName = "output.srt",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply {
                        file("input.mp4") {
                            subtitle(0) {
                                map = true
                                language = "eng"
                            }
                        }
                    },
                    output = OutputSection("output.srt").apply {
                        overwrite = true
                    }
                ),

                language = "eng"
            )
        ).newReferenceId()

        val listener = TestListener(delay = 10, mockExecConfig)

        listener.accept(task, overrideReporter)
        listener.getJob()?.join()

        val event = listener.getResult()
        assertTrue(event is ProcesserExtractResultEvent)
        assertSameReferenceId(task, event)
    }

    @Test
    @DisplayName("""
        Når en oppgave kommer med instruksjoner
        Hvis input fil er sat,
        Så:
            Skal vi kunne finne filen via instruksjonen
    """)
    fun verifies_input_file_is_found() {
        val testData = """
            {
                "data": {
                    "inputFile": "/src/scratch/Potato.mkv",
                    "instructions": {
                        "inputs": {
                            "inputs": [
                                {
                                    "streams": [
                                        {
                                            "language": "eng",
                                            "forced": false,
                                            "codec": "copy",
                                            "type": "SUBTITLE",
                                            "streamIndex": 0,
                                            "map": true
                                        }
                                    ],
                                    "path": "/src/scratch/Potato.mkv"
                                }
                            ]
                        },
                        "output": {
                            "path": "Potato-eng.ass",
                            "overwrite": true,
                            "progress": false,
                            "useWorkFile": true
                        }
                    },
                    "outputFileName": "Potato-eng.ass",
                    "outputFolderName": "Potato",
                    "language": "eng"
                },
                "referenceId": "44b4fa95-ac5a-4350-8f3a-9ab8e28a85b0",
                "taskId": "f6c89fd8-add7-47ed-b2fe-70c84d0518ad",
                "metadata": {
                    "created": "2026-03-22T17:58:34.168356696Z",
                    "derivedFromId": [
                        "25b8ad2a-f571-4af3-acff-2405145dace5"
                    ]
                }
            }
        """.trimIndent()

        val task = WGson.gson.fromJson(testData, ExtractSubtitleTask::class.java)
        assertDoesNotThrow {
            task.data.instructions.findPrimaryInput()
        }



    }
}