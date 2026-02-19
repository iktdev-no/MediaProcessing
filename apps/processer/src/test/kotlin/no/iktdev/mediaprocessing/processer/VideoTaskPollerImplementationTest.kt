package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.registry.TaskListenerRegistry
import no.iktdev.eventi.tasks.TaskPollerImplementation
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.registry.TaskTypeRegistry
import no.iktdev.mediaprocessing.processer.listeners.LinearVideoTaskListener
import no.iktdev.mediaprocessing.processer.listeners.SegmentedVideoTaskListener
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import no.iktdev.mediaprocessing.shared.database.InMemoryTaskStore
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.nio.charset.Charset
import java.time.Duration
import java.util.UUID

// -----------------------------------------------------------------------------
// Override listeners for testing
// -----------------------------------------------------------------------------

class LinearVideoTaskListenerOverride : LinearVideoTaskListener(
    coordinatorWebClient = getCoordinatorClient(),
    localProgress = LocalProgressCache(),
    fileUtil = TestUtils.getFileUtil(),
    executableConfig = TestUtils.getExecutableConfig(),
    processerProperties = getProcesserProperties()
) {
    val accepted = mutableListOf<UUID>()
    override fun accept(task: Task, reporter: TaskReporter): Boolean {
        return super.accept(task, reporter)
    }

    override suspend fun onTask(task: Task): Event? {
        accepted.add(task.taskId)
        return super.onTask(task)
    }
}

class SegmentedVideoTaskListenerOverride : SegmentedVideoTaskListener(
    coordinatorWebClient = getCoordinatorClient(),
    localProgress = LocalProgressCache(),
    fileUtil = TestUtils.getFileUtil(),
    executableConfig = TestUtils.getExecutableConfig(),
    processerProperties = getProcesserProperties()
) {
    val accepted = mutableListOf<UUID>()
    override fun accept(task: Task, reporter: TaskReporter): Boolean {
        return super.accept(task, reporter)
    }

    override suspend fun onTask(task: Task): Event? {
        accepted.add(task.taskId)
        return super.onTask(task)
    }
}

// -----------------------------------------------------------------------------
// Test suite
// -----------------------------------------------------------------------------

class VideoTaskPollerImplementationTest {

    private var store: InMemoryTaskStore = InMemoryTaskStore()
    private var poller: TaskPollerImplementation = object : TaskPollerImplementation(
        taskStore = store,
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
        TaskTypeRegistry.register(EncodeTask::class.java, MockTestTask::class.java)
        store.wipe()

        segmented = SegmentedVideoTaskListenerOverride()
        linear = LinearVideoTaskListenerOverride()
    }

    // -------------------------------------------------------------------------
    // Helper: Build EncodeTask from your JSON payload
    // -------------------------------------------------------------------------

    private fun buildTestSequenceEncodeTask(): EncodeTask {
        return EncodeTask(
            data = EncodeData(
                arguments = listOf(
                    "-map","0:v:0","-c:v","libx265","-crf","18","-preset","slow",
                    "-map","0:a:0","-c:a:0","copy"
                ),
                outputFileName = "Potato masters - S01E01 - Personal Rule.mp4",
                inputFile = "/src/scratch/Potato masters - S01E01 - Personal Rule.mkv"
            ),
        ).apply { newReferenceId() }
    }

    private fun buildTestLinearEncodeTask(): EncodeTask {
        return EncodeTask(
            data = EncodeData(
                arguments = listOf(
                    "-map","0:v:0","-c:v","copy","-crf","18","-preset","slow",
                    "-map","0:a:0","-c:a:0","copy"
                ),
                outputFileName = "Potato masters - S01E01 - Personal Rule.mp4",
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
        println("---- DEBUG ----")
        println("Task: $task")
        println("Task ID: ${task.taskId}")
        println("Listeners: ${TaskListenerRegistry.getListeners()}")
        println("Linear accepted: ${linear.accepted}")
        println("Segmented accepted: ${segmented.accepted}")
        println("Working dir: ${File(".").absolutePath}")
        println("User dir: ${System.getProperty("user.dir")}")
        println("OS: ${System.getProperty("os.name")}")
        println("Charset: ${Charset.defaultCharset()}")
        println("Temp dir: ${System.getProperty("java.io.tmpdir")}")
        println("---------------")

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
        val task = buildTestSequenceEncodeTask().copy(
            data = buildTestSequenceEncodeTask().data.copy(
                arguments = listOf("-vf", "scale=1280:720") // touches video → segmented
            )
        ).apply { newReferenceId() }

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
