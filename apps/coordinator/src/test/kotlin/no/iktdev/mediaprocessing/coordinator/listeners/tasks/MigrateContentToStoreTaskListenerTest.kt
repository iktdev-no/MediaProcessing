package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.MockFileSystemService
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MigrateContentToStoreTaskListenerTest {

    class MigrateContentToStoreTaskListenerTestImplementation: MigrateContentToStoreTaskListener() {

        var fs: FileSystemService? = null
        override fun getFileSystemService(): FileSystemService {
            return fs!!
        }
    }

    val listener = MigrateContentToStoreTaskListenerTestImplementation()

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
        val fs = MockFileSystemService().also {
            listener.fs = it
        }

        val content = MigrateToContentStoreTask.Data.SingleContent(
            cachedUri = "/tmp/source.mp4",
            storeUri = "/tmp/dest.mp4"
        )

        val result = listener.migrateVideo(fs, content)

        assertEquals(MigrateStatus.Completed, result.status)
        assertEquals("/tmp/dest.mp4", result.storedUri)
        assertEquals(1, fs.copied.size)
    }

    @Test
    @DisplayName(
        """
        Når migrateVideo kjøres
        Hvis copy feiler
        Så:
            returneres Failed
        """
    )
    fun migrateVideo_copyFails() {
        val fs = MockFileSystemService().apply { copyShouldFail = true }.also {
            listener.fs = it
        }


        val content = MigrateToContentStoreTask.Data.SingleContent(
            cachedUri = "/tmp/source.mp4",
            storeUri = "/tmp/dest.mp4"
        )

        val result = listener.migrateVideo(fs, content)

        assertEquals(MigrateStatus.Failed, result.status)
    }

    @Test
    @DisplayName(
        """
        Når migrateVideo kjøres
        Hvis copy lykkes men filene ikke er identiske
        Så:
            returneres Failed
        """
    )
    fun migrateVideo_mismatch() {
        val fs = MockFileSystemService().apply { identical = false }.also {
            listener.fs = it
        }

        val content = MigrateToContentStoreTask.Data.SingleContent(
            cachedUri = "/tmp/source.mp4",
            storeUri = "/tmp/dest.mp4"
        )

        val result = listener.migrateVideo(fs, content)

        assertEquals(MigrateStatus.Failed, result.status)
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
        val fs = MockFileSystemService().also {
            listener.fs = it
        }

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
        val fs = MockFileSystemService().also {
            listener.fs = it
        }

        val sub = MigrateToContentStoreTask.Data.SingleSubtitle(
            language = "en",
            cachedUri = "/tmp/a.srt",
            storeUri = "/tmp/b.srt"
        )

        val result = listener.migrateSubtitle(fs, listOf(sub))

        assertEquals(MigrateStatus.Completed, result.first().status)
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
        val fs = MockFileSystemService().also {
            listener.fs = it
        }

        val cover = MigrateToContentStoreTask.Data.SingleContent(
            cachedUri = "/tmp/c.jpg",
            storeUri = "/tmp/c2.jpg"
        )

        val result = listener.migrateCover(fs, listOf(cover))

        assertEquals(MigrateStatus.Completed, result.first().status)
    }

    // -------------------------------------------------------------------------
    // onTask
    // -------------------------------------------------------------------------

    @Test
    @DisplayName(
        """
        Når onTask kjøres
        Hvis alle migreringer lykkes
        Så:
            returneres Completed-event og cache slettes
        """
    )
    fun onTask_success() = runTest {
        val fs = MockFileSystemService().also {
            listener.fs = it
        }

        val task = MigrateToContentStoreTask(
            MigrateToContentStoreTask.Data(
                collection = "col",
                videoContent = MigrateToContentStoreTask.Data.SingleContent("/tmp/v", "/tmp/v2"),
                subtitleContent = listOf(
                    MigrateToContentStoreTask.Data.SingleSubtitle("en", "/tmp/s", "/tmp/s2")
                ),
                coverContent = listOf(
                    MigrateToContentStoreTask.Data.SingleContent("/tmp/c", "/tmp/c2")
                )
            )
        ).newReferenceId()

        val event = listener.onTask(task) as MigrateContentToStoreTaskResultEvent

        assertEquals(TaskStatus.Completed, event.status)
        assertEquals(3, fs.deleted.size)
    }

    @Test
    @DisplayName(
        """
        Når onTask kjøres
        Hvis en migrering feiler
        Så:
            returneres Failed-event og ingenting slettes
        """
    )
    fun onTask_failure() = runTest {
        val fs = MockFileSystemService().apply { copyShouldFail = true }.also {
            listener.fs = it
        }

        val task = MigrateToContentStoreTask(
            MigrateToContentStoreTask.Data(
                collection = "col",
                videoContent = MigrateToContentStoreTask.Data.SingleContent("/tmp/v", "/tmp/v2"),
                subtitleContent = emptyList(),
                coverContent = emptyList()
            )
        ).newReferenceId()

        val event = listener.onTask(task) as MigrateContentToStoreTaskResultEvent

        assertEquals(TaskStatus.Failed, event.status)
        assertEquals(0, fs.deleted.size)
    }
}
