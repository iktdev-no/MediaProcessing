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
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.system.measureTimeMillis

class VideoTaskListenerTest {

    class TestListener(val delay: Long, coordinatorClient: CoordinatorClient): VideoTaskListener(coordinatorClient) {
        fun getJob() = currentJob

        private var _result: Event? = null
        fun getResult(): Event? {
            return _result
        }
        override fun onComplete(task: Task, result: Event?) {
            this._result = result
        }

        override fun getFfmpeg(): FFmpeg {
            return MockFFmpeg(delayMillis = delay, listener =  MockFFmpeg.emptyListener())
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
        assertTrue(time <= (delay*2), "Expected onTask to wait less than ${(delay*2)} ms, waited for $time ms")

    }

}