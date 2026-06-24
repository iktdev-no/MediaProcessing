package no.iktdev.mediaprocessing.coordinator.listeners.tasks.transfer

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.files.FakeFile
import no.iktdev.mediaprocessing.MockFileSystemService
import no.iktdev.mediaprocessing.TestBase
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

open class TransferToStorageBaseListenerTest : TestBase() {

    // En enkel test-implementasjon for å teste abstrakt klasse
    class TestListener : TransferToStorageBaseListener() {
        var fs: FileSystemService? = null
        override fun getFileSystemService(): FileSystemService = fs!!

        // Trengs for å oppfylle TaskListener kontrakt
        override fun getWorkerId() = "test"
        override fun supports(task: Task) = false
        override suspend fun onTask(task: Task) = null
        override fun createIncompleteStateTaskEvent(task: Task, status: TaskStatus, exception: Exception?): Event {
            throw IllegalStateException("Should not be called")
        }
    }

    class FakeTaskReporter : TaskReporter {
        val events = mutableListOf<Event>()
        var completed = false
        var failed = false

        override fun markClaimed(taskId: UUID, workerId: String): no.iktdev.eventi.tasks.Result { return no.iktdev.eventi.tasks.Result.Success }
        override fun updateLastSeen(taskId: UUID): no.iktdev.eventi.tasks.Result { return no.iktdev.eventi.tasks.Result.Success }
        override fun markCompleted(taskId: UUID): no.iktdev.eventi.tasks.Result { completed = true; return no.iktdev.eventi.tasks.Result.Success }
        override fun markFailed(referenceId: UUID, taskId: UUID): no.iktdev.eventi.tasks.Result { failed = true; return no.iktdev.eventi.tasks.Result.Success }
        override fun markCancelled(referenceId: UUID, taskId: UUID): no.iktdev.eventi.tasks.Result { return no.iktdev.eventi.tasks.Result.Success }
        override fun updateProgress(referenceId: UUID, taskId: UUID, payload: Progress): no.iktdev.eventi.tasks.Result { return no.iktdev.eventi.tasks.Result.Success }
        override fun log(taskId: UUID, message: String) {}

        override fun publishEvent(event: Event): no.iktdev.eventi.tasks.Result {
            events.add(event)
            return Result.Success
        }
    }

    private val listener = TestListener()

    @Test
    fun `transfer bør kopiere fil og verifisere`() {
        val fs = MockFileSystemService().also {
            listener.fs = it
        }
        val source = TransferToStorageBaseListener.SourceFile("/tmp/src")
        val dest = TransferToStorageBaseListener.DestinationFile("/tmp/dest")
        (dest.destination as FakeFile).changeExist(false)

        listener.transfer(source, dest)

        assertEquals(1, fs.copied.size)
        assertEquals(1, fs.verified.size)
        assertEquals(1, fs.deleted.size) // Siden deleteSourceAfterVerify = true
    }
}