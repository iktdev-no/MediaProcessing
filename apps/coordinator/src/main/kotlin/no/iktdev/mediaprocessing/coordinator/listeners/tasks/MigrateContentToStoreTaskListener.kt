package no.iktdev.mediaprocessing.coordinator.listeners.tasks

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.coordinator.util.FileSystemService
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MigrateToContentStoreTask
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import no.iktdev.mediaprocessing.shared.common.silentTry
import org.jetbrains.annotations.VisibleForTesting
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.util.*

@Component
class MigrateContentToStoreTaskListener: TaskListener(TaskType.IO_INTENSIVE) {
    override fun getWorkerId(): String {
        return "${this::class.java.simpleName}-${taskType}-${UUID.randomUUID()}"
    }

    override fun supports(task: Task): Boolean {
        return task is MigrateToContentStoreTask
    }

    override suspend fun onTask(task: Task): Event? {
        val pickedTask = task as? MigrateToContentStoreTask ?: return null
        val fs = getFileSystemService()

        // Disse vil kaste exceptions hvis noe går galt
        val videoStatus = migrateVideo(fs, pickedTask.data.videoContent)
        val subtitleStatus = migrateSubtitle(fs, pickedTask.data.subtitleContent ?: emptyList())
        val coverStatus = migrateCover(fs, pickedTask.data.coverContent ?: emptyList())

        // Hvis vi kommer hit, har ingen migrering kastet exceptions → alt OK
        deleteCache(fs, pickedTask)

        return MigrateContentToStoreTaskResultEvent(
            status = TaskStatus.Completed,
            migrateData = MigrateContentToStoreTaskResultEvent.MigrateData(
                collection = pickedTask.data.collection,
                videoMigrate = videoStatus,
                subtitleMigrate = subtitleStatus,
                coverMigrate = coverStatus
            )
        ).producedFrom(task)
    }


    override fun createIncompleteStateTaskEvent(
        task: Task,
        status: TaskStatus,
        exception: Exception?
    ): Event {
        val message = when (status) {
            TaskStatus.Failed -> exception?.message ?: "Unknown error, see log"
            TaskStatus.Cancelled -> "Canceled"
            else -> ""
        }
        return MigrateContentToStoreTaskResultEvent(null, status, error = message)
    }

    private fun deleteCache(fs: FileSystemService, task: MigrateToContentStoreTask) {
        task.data.videoContent?.cachedUri?.let { silentTry { fs.delete(File(it)) } }
        task.data.subtitleContent?.forEach { silentTry { fs.delete(File(it.cachedUri)) } }
        task.data.coverContent?.forEach { silentTry { fs.delete(File(it.cachedUri)) } }
    }


    internal fun migrateVideo(
        fs: FileSystemService,
        videoContent: MigrateToContentStoreTask.Data.SingleContent?
    ): MigrateContentToStoreTaskResultEvent.FileMigration {

        if (videoContent == null) {
            return MigrateContentToStoreTaskResultEvent.FileMigration(null, MigrateStatus.NotPresent)
        }

        val source = File(videoContent.cachedUri)
        val destination = File(videoContent.storeUri)

        // 1. Hvis destinasjonen finnes, sjekk identitet
        if (destination.exists()) {
            if (fs.areIdentical(source, destination)) {
                // Skip – allerede migrert
                return MigrateContentToStoreTaskResultEvent.FileMigration(
                    destination.absolutePath,
                    MigrateStatus.Completed
                )
            } else {
                throw IllegalStateException(
                    "Destination file already exists but is not identical: $destination"
                )
            }
        }

        // 2. Utfør kopiering
        if (!fs.copy(source, destination)) {
            throw IllegalStateException("File could not be copied to: $destination from $source")
        }

        // 3. Verifiser kopien (optional)
        if (!fs.areIdentical(source, destination)) {
            throw IllegalStateException("Copied file is not identical to source: $destination")
        }

        return MigrateContentToStoreTaskResultEvent.FileMigration(
            destination.absolutePath,
            MigrateStatus.Completed
        )
    }

    @VisibleForTesting
    internal fun migrateSubtitle(
        fs: FileSystemService,
        subtitleContents: List<MigrateToContentStoreTask.Data.SingleSubtitle>
    ): List<MigrateContentToStoreTaskResultEvent.SubtitleMigration> {

        if (subtitleContents.isEmpty()) {
            return listOf(
                MigrateContentToStoreTaskResultEvent.SubtitleMigration(
                    language = null,
                    storedUri = null,
                    status = MigrateStatus.NotPresent
                )
            )
        }

        return subtitleContents.map { subtitle ->
            val source = File(subtitle.cachedUri)
            val destination = File(subtitle.storeUri)

            // 1. Hvis destinasjonen finnes
            if (destination.exists()) {
                if (fs.areIdentical(source, destination)) {
                    return@map MigrateContentToStoreTaskResultEvent.SubtitleMigration(
                        subtitle.language,
                        destination.absolutePath,
                        MigrateStatus.Completed
                    )
                } else {
                    throw IllegalStateException(
                        "Destination subtitle exists but is not identical: ${destination.absolutePath}"
                    )
                }
            }

            // 2. Kopier
            if (!fs.copy(source, destination)) {
                throw IllegalStateException(
                    "Failed to copy subtitle ${subtitle.language} from $source to $destination"
                )
            }

            // 3. Verifiser
            if (!fs.areIdentical(source, destination)) {
                throw IllegalStateException(
                    "Copied subtitle ${subtitle.language} is not identical: ${destination.absolutePath}"
                )
            }

            // 4. OK
            MigrateContentToStoreTaskResultEvent.SubtitleMigration(
                subtitle.language,
                destination.absolutePath,
                MigrateStatus.Completed
            )
        }
    }

    @VisibleForTesting
    internal fun migrateCover(
        fs: FileSystemService,
        coverContents: List<MigrateToContentStoreTask.Data.SingleContent>
    ): List<MigrateContentToStoreTaskResultEvent.FileMigration> {

        if (coverContents.isEmpty()) {
            return listOf(
                MigrateContentToStoreTaskResultEvent.FileMigration(
                    storedUri = null,
                    status = MigrateStatus.NotPresent
                )
            )
        }

        return coverContents.map { cover ->
            val source = File(cover.cachedUri)
            val destination = File(cover.storeUri)

            // 1. Hvis destinasjonen finnes
            if (destination.exists()) {
                if (fs.areIdentical(source, destination)) {
                    return@map MigrateContentToStoreTaskResultEvent.FileMigration(
                        destination.absolutePath,
                        MigrateStatus.Completed
                    )
                } else {
                    throw IllegalStateException(
                        "Destination cover exists but is not identical: ${destination.absolutePath}"
                    )
                }
            }

            // 2. Kopier
            if (!fs.copy(source, destination)) {
                throw IllegalStateException(
                    "Failed to copy cover from $source to $destination"
                )
            }

            // 3. Verifiser
            if (!fs.areIdentical(source, destination)) {
                throw IllegalStateException(
                    "Copied cover is not identical: ${destination.absolutePath}"
                )
            }

            // 4. OK
            MigrateContentToStoreTaskResultEvent.FileMigration(
                destination.absolutePath,
                MigrateStatus.Completed
            )
        }
    }



    open fun getFileSystemService(): FileSystemService {
        return DefaultFileSystemService()
    }

    class DefaultFileSystemService : FileSystemService {
        override fun copy(source: File, destination: File): Boolean {
            return try {
                source.copyTo(destination, overwrite = true)
                true
            } catch (e: Exception) {
                false
            }
        }

        override fun areIdentical(a: File, b: File): Boolean {
            return Files.mismatch(a.toPath(), b.toPath()) == -1L
        }

        override fun delete(file: File) {
            file.delete()
        }
    }

}