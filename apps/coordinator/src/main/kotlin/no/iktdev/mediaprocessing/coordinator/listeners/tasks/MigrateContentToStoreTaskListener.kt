package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.coordinator.services.DefaultFileSystemService
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import no.iktdev.mediaprocessing.shared.common.silentTry
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.util.*

@Component
class MigrateContentToStoreTaskListener : TaskListener(TaskType.IO_INTENSIVE) {

    override fun getWorkerId(): String =
        "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun supports(task: Task): Boolean =
        task is MigrateToContentStoreTask

    override suspend fun onTask(task: Task): Event? {
        val picked = task as? MigrateToContentStoreTask ?: return null
        val fs = getFileSystemService()

        val video = migrateVideo(fs, picked.data.videoContent)
        val subs = migrateSubtitle(fs, picked.data.subtitleContent ?: emptyList())
        val covers = migrateCover(fs, picked.data.coverContent ?: emptyList())

        deleteCache(fs, picked)

        return MigrateContentToStoreTaskResultEvent(
            status = TaskStatus.Completed,
            migrateData = MigrateContentToStoreTaskResultEvent.MigrateData(
                collection = picked.data.collection,
                videoMigrate = video,
                subtitleMigrate = subs,
                coverMigrate = covers
            )
        ).producedFrom(task)
    }

    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        val message = when (status) {
            TaskStatus.Failed -> exception?.message ?: "Unknown error"
            TaskStatus.Cancelled -> "Canceled"
            else -> ""
        }

        return MigrateContentToStoreTaskResultEvent(
            migrateData = null,
            status = status,
            error = message
        ).producedFrom(task)
    }

    private fun deleteCache(fs: FileSystemService, task: MigrateToContentStoreTask) {
        task.data.videoContent?.cachedUri?.let { silentTry { fs.delete(File(it)) } }
        task.data.subtitleContent?.forEach { silentTry { fs.delete(File(it.cachedUri)) } }
        task.data.coverContent?.forEach { silentTry { fs.delete(File(it.cachedUri)) } }
    }

    // -------------------------------------------------------------------------
    // MIGRATION HELPERS
    // -------------------------------------------------------------------------

    private fun migrateFile(fs: FileSystemService, source: File, destination: File) {
        if (destination.exists()) {
            try {
                fs.verifyIdentical(source, destination)
                return
            } catch (e: FileServiceException.VerificationFailed) {
                throw FileServiceException.DestinationExistsButDifferent(source, destination)
            }
        }

        fs.copy(source, destination)
        fs.verifyIdentical(source, destination)
    }


    internal fun migrateVideo(
        fs: FileSystemService,
        content: MigrateToContentStoreTask.Data.SingleContent?
    ): MigrateContentToStoreTaskResultEvent.FileMigration {

        if (content == null) {
            return MigrateContentToStoreTaskResultEvent.FileMigration(null, MigrateStatus.NotPresent)
        }

        val source = File(content.cachedUri)
        val dest = File(content.storeUri)

        migrateFile(fs, source, dest)

        return MigrateContentToStoreTaskResultEvent.FileMigration(
            storedUri = dest.absolutePath,
            status = MigrateStatus.Completed
        )
    }

    internal fun migrateSubtitle(
        fs: FileSystemService,
        subs: List<MigrateToContentStoreTask.Data.SingleSubtitle>
    ): List<MigrateContentToStoreTaskResultEvent.SubtitleMigration> {

        if (subs.isEmpty()) {
            return listOf(
                MigrateContentToStoreTaskResultEvent.SubtitleMigration(
                    language = null,
                    storedUri = null,
                    status = MigrateStatus.NotPresent
                )
            )
        }

        return subs.map { sub ->
            val source = File(sub.cachedUri)
            val dest = File(sub.storeUri)

            migrateFile(fs, source, dest)

            MigrateContentToStoreTaskResultEvent.SubtitleMigration(
                language = sub.language,
                storedUri = dest.absolutePath,
                status = MigrateStatus.Completed
            )
        }
    }

    internal fun migrateCover(
        fs: FileSystemService,
        covers: List<MigrateToContentStoreTask.Data.SingleContent>
    ): List<MigrateContentToStoreTaskResultEvent.FileMigration> {

        if (covers.isEmpty()) {
            return listOf(
                MigrateContentToStoreTaskResultEvent.FileMigration(
                    storedUri = null,
                    status = MigrateStatus.NotPresent
                )
            )
        }

        return covers.map { cover ->
            val source = File(cover.cachedUri)
            val dest = File(cover.storeUri)

            migrateFile(fs, source, dest)

            MigrateContentToStoreTaskResultEvent.FileMigration(
                storedUri = dest.absolutePath,
                status = MigrateStatus.Completed
            )
        }
    }

    open fun getFileSystemService(): FileSystemService =
        DefaultFileSystemService()


}
