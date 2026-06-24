package no.iktdev.mediaprocessing.coordinator.listeners.tasks.transfer

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.Result
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.files.FakeFile
import no.iktdev.mediaprocessing.MockFileSystemService
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.transfer.VideoTransferredResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer.VideoTransferTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class TransferVideoToStorageTaskListenerTest : TransferToStorageBaseListenerTest() {

    class InternalTestListener(val fs: FileSystemService) : TransferVideoToStorageTaskListener() {
        override fun getFileSystemService(): FileSystemService {
            return fs
        }
    }


    @Test
    @DisplayName("Når video overføres, hvis suksess, returneres Completed")
    fun `video_success`() = runTest {
        val destFile = FakeFile("/dest/build/potet.mp4")
        val task = VideoTransferTask(UUID.randomUUID(), "col", "/src/build/potet.mp4", destFile.absolutePath)
        val reporter = FakeTaskReporter()
        destFile.changeExist(false)

        val fs = MockFileSystemService().apply {
        }

        task.storeUri.let { FakeFile(it).changeExist(false) }

        val result = InternalTestListener(fs).onTask(task) as VideoTransferredResultEvent

        assertEquals(TaskStatus.Completed, result.status)
        assertEquals(destFile.absolutePath, result.fileUri)
    }

    @Test
    @DisplayName("Når video overføres, hvis identisk, returneres Skipped")
    fun `video_skipped`() = runTest {
        // Her kan du mocke fs til å kaste FilesAreIdentical
        val task = VideoTransferTask(UUID.randomUUID(), "col", "/src", "/dest")
        val fs = MockFileSystemService()


        val result = InternalTestListener(fs).onTask(task) as VideoTransferredResultEvent

        assertEquals(TaskStatus.Skipped, result.status)
    }
}