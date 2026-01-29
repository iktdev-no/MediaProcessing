package no.iktdev.mediaprocessing.processer.listeners

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.eventi.tasks.TaskTypeRegistry
import no.iktdev.mediaprocessing.ffmpeg.FFmpeg
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.system.measureTimeMillis

class SubtitleTaskListenerTest {

    class TestListener(val delay: Long): SubtitleTaskListener() {
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
        override fun markCompleted(taskId: UUID) {}
        override fun markFailed(taskId: UUID) {}
        override fun updateProgress(taskId: UUID, progress: Int) {}
        override fun log(taskId: UUID, message: String) {}
        override fun publishEvent(event: Event) {

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
                arguments = listOf("-y"),
                language = "eng"
            )
        ).newReferenceId()

        val listener = TestListener(delay)

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

}