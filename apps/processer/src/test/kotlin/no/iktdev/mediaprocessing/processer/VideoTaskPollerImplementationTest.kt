package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.registry.TaskListenerRegistry
import no.iktdev.eventi.tasks.TaskPollerImplementation
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.registry.TaskTypeRegistry
import no.iktdev.eventi.tasks.TaskValidator
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.listeners.LinearVideoTaskListener
import no.iktdev.mediaprocessing.processer.listeners.SegmentedVideoTaskListener
import no.iktdev.mediaprocessing.processer.listeners.VideoTaskListener
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.LinearEncodeTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.SegmentedEncodeTask
import no.iktdev.mediaprocessing.shared.common.model.task.data.LinearEncodeData
import no.iktdev.mediaprocessing.shared.common.model.task.data.DefaultEncodeData
import no.iktdev.mediaprocessing.shared.database.InMemoryTaskStore
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.Duration
import java.util.UUID

// -----------------------------------------------------------------------------
// Override listeners for testing
// -----------------------------------------------------------------------------

class LinearVideoTaskListenerOverride(executableConfig: ExecutablesConfig) : LinearVideoTaskListener(
    coordinatorWebClient = getCoordinatorClient(),
    localProgress = LocalProgressCache(),
    fileUtil = TestUtils.getFileUtil(),
    executableConfig = executableConfig,
    processerProperties = getProcesserProperties()
) {
    val accepted = mutableListOf<UUID>()
    override fun accept(task: Task, reporter: TaskReporter, validator: TaskValidator?): Boolean {
        return super.accept(task, reporter, validator)
    }

    override suspend fun onTask(task: Task): Event? {
        accepted.add(task.taskId)
        return super.onTask(task)
    }
}

class SegmentedVideoTaskListenerOverride(executableConfig: ExecutablesConfig) : SegmentedVideoTaskListener(
    coordinatorWebClient = getCoordinatorClient(),
    localProgress = LocalProgressCache(),
    fileUtil = TestUtils.getFileUtil(),
    executableConfig = executableConfig,
) {
    val accepted = mutableListOf<UUID>()
    override fun accept(task: Task, reporter: TaskReporter, validator: TaskValidator?): Boolean {
        return super.accept(task, reporter, validator)
    }

    override suspend fun onTask(task: Task): Event? {
        accepted.add(task.taskId)
        return super.onTask(task)
    }
}

// -----------------------------------------------------------------------------
// Test suite
// -----------------------------------------------------------------------------

class VideoTaskPollerImplementationTest : TestBase() {

    private var store: InMemoryTaskStore = InMemoryTaskStore()
    private var poller: TaskPollerImplementation = object : TaskPollerImplementation(
        taskStore = store,
        lifecycleStore = defaultLifecycleStore,
        reporterFactory = { getTaskReporter() }
    ) {
        override var backoff: Duration = Duration.ofMillis(1)
    }

    private lateinit var linear: LinearVideoTaskListenerOverride
    private lateinit var segmented: SegmentedVideoTaskListenerOverride

    @BeforeEach
    fun setup() {
        TaskListenerRegistry.wipe()
        TaskTypeRegistry.wipe()
        TaskTypeRegistry.register(
            LinearEncodeTask::class.java,
            SegmentedEncodeTask::class.java,
            MockTestTask::class.java
        )
        store.wipe()
        VideoTaskListener.removeListeners()
        segmented = SegmentedVideoTaskListenerOverride(mockExecConfig)
        linear = LinearVideoTaskListenerOverride(mockExecConfig)
    }

    // -------------------------------------------------------------------------
    // Helper: Build EncodeTask from your JSON payload
    // -------------------------------------------------------------------------

    private fun buildTestSequenceEncodeTask(): SegmentedEncodeTask {
        return SegmentedEncodeTask(
            data = DefaultEncodeData(
                videoInstruction = FFmpegInstructions(
                    inputs = InputSection().apply {
                        file("/src/scratch/Potato masters - S01E01 - Personal Rule.mkv") {
                            video(0) {
                                map = true
                                codec = VideoCodec.Vvc()
                                options.crf = 18
                                options.preset = "slow"
                            }
                        }
                    },
                    output = OutputSection("Potato masters - S01E01 - Personal Rule.mp4").apply {
                        overwrite = true
                        useWorkFile = true
                    }
                ),
                audioInstructions = listOf(
                    FFmpegInstructions(
                        inputs = InputSection().apply {
                            file("/src/scratch/Potato masters - S01E01 - Personal Rule.mkv") {
                                audio(0) {
                                    map = true
                                    codec = AudioCodec.Copy()
                                }
                            }
                        },
                        output = OutputSection("Potato masters - S01E01 - Personal Rule.mka").apply {
                            overwrite = true
                        }
                    )
                ),
                outputFileName = "Potato masters - S01E01 - Personal Rule.mp4",
                outputFolderName = "Potato masters - S01E01 - Personal Rule",
                inputFile = "/src/scratch/Potato masters - S01E01 - Personal Rule.mkv"
            ),
        ).apply { newReferenceId() }
    }

    private fun buildTestLinearEncodeTask(): LinearEncodeTask {
        return LinearEncodeTask(
            data = DefaultEncodeData(
                videoInstruction = FFmpegInstructions(
                    inputs = InputSection().apply {
                        file("/src/scratch/Potato masters - S01E01 - Personal Rule.mkv") {
                            video(0) {
                                map = true
                                codec = VideoCodec.Vvc()
                                options.crf = 18
                                options.preset = "slow"
                            }
                            audio(0) {
                                map = true
                                codec = AudioCodec.Copy()
                            }
                        }
                    },
                    output = OutputSection("Potato masters - S01E01 - Personal Rule.mp4").apply {
                        overwrite = true
                        useWorkFile = true
                    }
                ),
                audioInstructions = listOf(
                    FFmpegInstructions(
                        inputs = InputSection().apply {
                            file("/src/scratch/Potato masters - S01E01 - Personal Rule.mkv") {
                                audio(0) {
                                    map = true
                                    codec = AudioCodec.Copy()
                                }
                            }
                        },
                        output = OutputSection("Potato masters - S01E01 - Personal Rule.mp4").apply {
                            overwrite = true
                            useWorkFile = true
                        }
                    )
                ),
                outputFileName = "Potato masters - S01E01 - Personal Rule.mp4",
                outputFolderName = "Potato masters - S01E01 - Personal Rule",
                inputFile = "/src/scratch/Potato masters - S01E01 - Personal Rule.mkv"
            ),
        ).apply { newReferenceId() }
    }


    // -------------------------------------------------------------------------
    // TEST 1: Linear listener should accept this task
    // -------------------------------------------------------------------------

    @Test
    fun `Linear listener should accept this encode task`() = runBlocking {
        val task = buildTestLinearEncodeTask()
        store.persist(task)

        poller.pollOnce()
        yield()
        assertTrue { TaskListenerRegistry.getListeners().size == 2 }
        assertTrue(linear.accepted.contains(task.taskId))
        assertFalse(segmented.accepted.contains(task.taskId))
    }

    // -------------------------------------------------------------------------
    // TEST 2: Segmented listener should accept when strategy is Segmented
    // -------------------------------------------------------------------------

    @Test
    fun `Segmented listener should accept when strategy is Segmented`() = runBlocking {
        val task = buildTestSequenceEncodeTask()
        store.persist(task)

        poller.pollOnce()
        yield()
        assertTrue { TaskListenerRegistry.getListeners().size == 2 }
        assertTrue(segmented.accepted.contains(task.taskId))
        assertFalse(linear.accepted.contains(task.taskId))
    }

    // -------------------------------------------------------------------------
    // TEST 3: No listener should accept unsupported task types
    // -------------------------------------------------------------------------
    class MockTestTask : Task()

    @Test
    fun `No listener should accept non-encode tasks`() = runBlocking {

        store.persist(MockTestTask().newReferenceId())

        poller.pollOnce()
        yield()

        assertTrue(linear.accepted.isEmpty())
        assertTrue(segmented.accepted.isEmpty())
    }
}
