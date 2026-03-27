package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import mu.KotlinLogging
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.services.DefaultFileSystemService
import no.iktdev.mediaprocessing.coordinator.util.FileServiceException
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import no.iktdev.mediaprocessing.shared.common.silentTry
import org.springframework.stereotype.Component
import java.util.*

@Component
class MigrateContentToStoreTaskListener : TaskListener(TaskType.IO_INTENSIVE) {

    val log = KotlinLogging.logger {}


    override fun getWorkerId(): String =
        "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"

    override fun supports(task: Task): Boolean =
        task is MigrateToContentStoreTask

    override suspend fun onTask(task: Task): Event? {
        val picked = task as? MigrateToContentStoreTask ?: return null
        val fs = getFileSystemService()

        val video = migrateVideo(fs, picked.data.videoContent)
        val subs = migrateSubtitle(fs, picked.data.subtitleContent ?: emptyList())
        val covers = migrateCover(fs, picked.data.coverContent)

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
        task.data.videoContent?.cachedUri?.let { silentTry { fs.delete(IFile(it)) } }
        task.data.subtitleContent?.forEach { silentTry { fs.delete(IFile(it.cachedUri)) } }
        // task.data.coverContent?.let { silentTry { fs.delete(File(it.cachedUri)) } } // NOTE: Covers takes up little to no space, if this is to be enabled, we will need to move it back into subfolder!
    }

    // -------------------------------------------------------------------------
    // MIGRATION HELPERS
    // -------------------------------------------------------------------------

    private fun migrateFile(fs: FileSystemService, source: IFile, destination: IFile) {
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
        content: ContentMigrationPlan.SingleContent?
    ): MigrateContentToStoreTaskResultEvent.FileMigration {

        if (content == null) {
            return MigrateContentToStoreTaskResultEvent.FileMigration(null, MigrateStatus.NotPresent)
        }

        val source = IFile(content.cachedUri)
        val dest = IFile(content.storeUri)

        migrateFile(fs, source, dest)

        return MigrateContentToStoreTaskResultEvent.FileMigration(
            storedUri = dest.absolutePath,
            status = MigrateStatus.Completed
        )
    }

    internal fun migrateSubtitle(
        fs: FileSystemService,
        subs: List<ContentMigrationPlan.SingleSubtitle>
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
            val source = IFile(sub.cachedUri)
            val dest = IFile(sub.storeUri)

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
        cover: ContentMigrationPlan.SingleContent?
    ): MigrateContentToStoreTaskResultEvent.FileMigration {

        return cover?.let { cover ->
            val source = IFile(cover.cachedUri)
            val dest = IFile(cover.storeUri)

            if (dest.exists()) {
                log.info { "Cover already exists under ${dest.parentFile.absolutePath}" }
                return MigrateContentToStoreTaskResultEvent.FileMigration(
                    storedUri = dest.absolutePath,
                    status = MigrateStatus.Skipped
                )
            }
            migrateFile(fs, source, dest)

            MigrateContentToStoreTaskResultEvent.FileMigration(
                storedUri = dest.absolutePath,
                status = MigrateStatus.Completed
            )
        } ?: MigrateContentToStoreTaskResultEvent.FileMigration(
            storedUri = null,
            status = MigrateStatus.NotPresent
        )
    }

    open fun getFileSystemService(): FileSystemService =
        DefaultFileSystemService()


}
