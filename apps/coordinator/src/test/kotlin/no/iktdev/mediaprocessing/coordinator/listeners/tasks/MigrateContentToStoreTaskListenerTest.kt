package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.MockFileSystemService
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

class MigrateContentToStoreTaskListenerTest {

    // -------------------------------------------------------------------------
    // Fake Reporter
    // -------------------------------------------------------------------------

    class FakeTaskReporter : TaskReporter {
        val events = mutableListOf<Event>()
        var completed = false
        var failed = false

        override fun markClaimed(taskId: UUID, workerId: String): Result { return Result.Success }
        override fun updateLastSeen(taskId: UUID): Result { return Result.Success }
        override fun markCompleted(taskId: UUID): Result { completed = true; return Result.Success }
        override fun markFailed(referenceId: UUID, taskId: UUID): Result { failed = true; return Result.Success }
        override fun markCancelled(referenceId: UUID, taskId: UUID): Result { return Result.Success }
        override fun updateProgress(referenceId: UUID, taskId: UUID, payload: Progress): Result { return Result.Success }
        override fun log(taskId: UUID, message: String) {}

        override fun publishEvent(event: Event): Result {
            events.add(event)
            return Result.Success
        }
    }

    // -------------------------------------------------------------------------
    // Listener with injectable FS
    // -------------------------------------------------------------------------

    class TestListener : MigrateContentToStoreTaskListener() {
        var fs: FileSystemService? = null
        override fun getFileSystemService(): FileSystemService = fs!!
    }

    private val listener = TestListener()

    // -------------------------------------------------------------------------
    // migrateVideo
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når migrateVideo kjøres
        Hvis copy lykkes og filene er identiske
        Så:
            returneres Completed
        """
    )
    fun migrateVideo_success() {
        val fs = MockFileSystemService().also { listener.fs = it }
        val content = ContentMigrationPlan.SingleContent("/tmp/source", "/tmp/dest")

        val result = listener.migrateVideo(fs, content)

        assertEquals(MigrateStatus.Completed, result.status)
        assertEquals("/tmp/dest", result.storedUri)
        assertEquals(1, fs.copied.size)
        assertEquals(1, fs.verified.size)
    }

    @Test
    @DisplayName(
        """
        Når migrateVideo kjøres
        Hvis copy feiler
        Så:
            kastes exception
        """
    )
    fun migrateVideo_copyFails() {
        val fs = MockFileSystemService().apply { copyShouldFail = true }.also { listener.fs = it }
        val content = ContentMigrationPlan.SingleContent("/tmp/source", "/tmp/dest")

        assertThrows<FileServiceException.CopyFailed> {
            listener.migrateVideo(fs, content)
        }
    }

    @Test
    @DisplayName(
        """
        Når migrateVideo kjøres
        Hvis filene ikke er identiske
        Så:
            kastes exception
        """
    )
    fun migrateVideo_mismatch() {
        val fs = MockFileSystemService().apply { identical = false }.also { listener.fs = it }
        val content = ContentMigrationPlan.SingleContent("/tmp/source", "/tmp/dest")

        assertThrows<FileServiceException.VerificationFailed> {
            listener.migrateVideo(fs, content)
        }
    }

    @Test
    @DisplayName(
        """
        Når migrateVideo kjøres
        Hvis content er null
        Så:
            returneres NotPresent
        """
    )
    fun migrateVideo_null() {
        val fs = MockFileSystemService().also { listener.fs = it }

        val result = listener.migrateVideo(fs, null)

        assertEquals(MigrateStatus.NotPresent, result.status)
    }

    // -------------------------------------------------------------------------
    // migrateSubtitle
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når migrateSubtitle kjøres
        Hvis listen er tom
        Så:
            returneres NotPresent
        """
    )
    fun migrateSubtitle_empty() {
        val fs = MockFileSystemService().also { listener.fs = it }

        val result = listener.migrateSubtitle(fs, emptyList())

        assertEquals(1, result.size)
        assertEquals(MigrateStatus.NotPresent, result.first().status)
    }

    @Test
    @DisplayName(
        """
        Når migrateSubtitle kjøres
        Hvis copy lykkes og filene er identiske
        Så:
            returneres Completed
        """
    )
    fun migrateSubtitle_success() {
        val fs = MockFileSystemService().also { listener.fs = it }
        val sub = ContentMigrationPlan.SingleSubtitle("en", "/tmp/a", "/tmp/b")

        val result = listener.migrateSubtitle(fs, listOf(sub))

        assertEquals(MigrateStatus.Completed, result.first().status)
    }

    @Test
    @DisplayName(
        """
        Når migrateSubtitle kjøres
        Hvis filene ikke er identiske
        Så:
            kastes exception
        """
    )
    fun migrateSubtitle_mismatch() {
        val fs = MockFileSystemService().apply { identical = false }.also { listener.fs = it }
        val sub = ContentMigrationPlan.SingleSubtitle("en", "/tmp/a", "/tmp/b")

        assertThrows<FileServiceException.VerificationFailed> {
            listener.migrateSubtitle(fs, listOf(sub))
        }
    }

    @Test
    @DisplayName(
        """
        Når migrateSubtitle kjøres
        Hvis copy feiler
        Så:
            kastes exception
        """
    )
    fun migrateSubtitle_copyFails() {
        val fs = MockFileSystemService().apply { copyShouldFail = true }.also { listener.fs = it }
        val sub = ContentMigrationPlan.SingleSubtitle("en", "/tmp/a", "/tmp/b")

        assertThrows<FileServiceException.CopyFailed> {
            listener.migrateSubtitle(fs, listOf(sub))
        }
    }

    // -------------------------------------------------------------------------
    // migrateCover
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når migrateCover kjøres
        Hvis copy lykkes og filene er identiske
        Så:
            returneres Completed
        """
    )
    fun migrateCover_success() {
        val fs = MockFileSystemService().also { listener.fs = it }
        val cover = ContentMigrationPlan.SingleContent("/tmp/c", "/tmp/c2")

        val result = listener.migrateCover(fs, listOf(cover))

        assertEquals(MigrateStatus.Completed, result.first().status)
    }

    @Test
    @DisplayName(
        """
        Når migrateCover kjøres
        Hvis filene ikke er identiske
        Så:
            kastes exception
        """
    )
    fun migrateCover_mismatch() {
        val fs = MockFileSystemService().apply { identical = false }.also { listener.fs = it }
        val cover = ContentMigrationPlan.SingleContent("/tmp/c", "/tmp/c2")

        assertThrows<FileServiceException.VerificationFailed> {
            listener.migrateCover(fs, listOf(cover))
        }
    }

    @Test
    @DisplayName(
        """
        Når migrateCover kjøres
        Hvis copy feiler
        Så:
            kastes exception
        """
    )
    fun migrateCover_copyFails() {
        val fs = MockFileSystemService().apply { copyShouldFail = true }.also { listener.fs = it }
        val cover = ContentMigrationPlan.SingleContent("/tmp/c", "/tmp/c2")

        assertThrows<FileServiceException.CopyFailed> {
            listener.migrateCover(fs, listOf(cover))
        }
    }

    // -------------------------------------------------------------------------
    // accept()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når accept() kjøres
        Hvis alle migreringer lykkes
        Så:
            publiseres Completed-event og cache slettes
        """
    )
    fun accept_success() = runTest {
        val fs = MockFileSystemService().also { listener.fs = it }
        val reporter = FakeTaskReporter()

        val task = MigrateToContentStoreTask(
            ContentMigrationPlan(
                "col",
                videoContent = ContentMigrationPlan.SingleContent("/tmp/v", "/tmp/v2"),
                subtitleContent = listOf(
                    ContentMigrationPlan.SingleSubtitle("en", "/tmp/s", "/tmp/s2")
                ),
                coverContent = listOf(
                    ContentMigrationPlan.SingleContent("/tmp/c", "/tmp/c2")
                )
            )
        ).newReferenceId()

        listener.accept(task, reporter)
        listener.currentJob?.join()

        val event = reporter.events.first() as MigrateContentToStoreTaskResultEvent

        assertTrue(reporter.completed)
        assertEquals(TaskStatus.Completed, event.status)
        assertEquals(3, fs.deleted.size)
    }

    @Test
    @DisplayName(
        """
        Når accept() kjøres
        Hvis migrateVideo kaster exception
        Så:
            publiseres Failed-event og cache slettes ikke
        """
    )
    fun accept_failure() = runTest {
        val fs = MockFileSystemService().apply { copyShouldFail = true }.also { listener.fs = it }
        val reporter = FakeTaskReporter()

        val task = MigrateToContentStoreTask(
            ContentMigrationPlan(
                "col",
                videoContent = ContentMigrationPlan.SingleContent("/tmp/v", "/tmp/v2"),
                subtitleContent = emptyList(),
                coverContent = emptyList()
            )
        ).newReferenceId()

        listener.accept(task, reporter)
        listener.currentJob?.join()

        val event = reporter.events.first() as MigrateContentToStoreTaskResultEvent

        assertTrue(reporter.failed)
        assertEquals(TaskStatus.Failed, event.status)
        assertEquals(0, fs.deleted.size)
    }

    @Test
    @DisplayName(
        """
        Når accept() kjøres
        Hvis sletting feiler
        Så:
            publiseres fortsatt Completed-event
        """
    )
    fun accept_deleteFails() = runTest {
        val fs = MockFileSystemService().apply { deleteShouldFail = true }.also { listener.fs = it }
        val reporter = FakeTaskReporter()

        val task = MigrateToContentStoreTask(
            ContentMigrationPlan(
                "col",
                videoContent = ContentMigrationPlan.SingleContent("/tmp/v", "/tmp/v2"),
                subtitleContent = emptyList(),
                coverContent = emptyList()
            )
        ).newReferenceId()

        listener.accept(task, reporter)
        listener.currentJob?.join()

        val event = reporter.events.first() as MigrateContentToStoreTaskResultEvent

        assertTrue(reporter.completed)
        assertEquals(TaskStatus.Completed, event.status)
    }
}
