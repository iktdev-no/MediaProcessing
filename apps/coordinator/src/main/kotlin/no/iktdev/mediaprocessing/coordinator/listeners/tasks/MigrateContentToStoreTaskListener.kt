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

        val videoStatus = migrateVideo(fs, pickedTask.data.videoContent)
        val subtitleStatus = migrateSubtitle(fs, pickedTask.data.subtitleContent ?: emptyList())
        val coverStatus = migrateCover(fs, pickedTask.data.coverContent ?: emptyList())

        var status = TaskStatus.Completed
        if (videoStatus.status != MigrateStatus.Failed &&
            subtitleStatus.none { it.status == MigrateStatus.Failed } &&
            coverStatus.none { it.status == MigrateStatus.Failed })
        {
            pickedTask.data.videoContent?.cachedUri?.let { File(it) }?.let {
                silentTry { fs.delete(it) }
            }
            pickedTask.data.subtitleContent?.map { File(it.cachedUri) }?.forEach {
                silentTry { fs.delete(it) }
            }
            pickedTask.data.coverContent?.map { File(it.cachedUri) }?.forEach {
                silentTry { fs.delete(it) }
            }
        } else {
            status = TaskStatus.Failed
        }


        val completedEvent = MigrateContentToStoreTaskResultEvent(
            status = status,
            collection = pickedTask.data.collection,
            videoMigrate = videoStatus,
            subtitleMigrate = subtitleStatus,
            coverMigrate = coverStatus
        ).producedFrom(task)

        return completedEvent
    }

    @VisibleForTesting
    internal fun migrateVideo(fs: FileSystemService, videoContent: MigrateToContentStoreTask.Data.SingleContent?): MigrateContentToStoreTaskResultEvent.FileMigration {
        if (videoContent == null) return MigrateContentToStoreTaskResultEvent.FileMigration(null, MigrateStatus.NotPresent)
        val source = File(videoContent.cachedUri)
        val destination = File(videoContent.storeUri)
        return try {
            if (!fs.copy(source, destination)) {
                return MigrateContentToStoreTaskResultEvent.FileMigration(null, MigrateStatus.Failed)
            }

            if (!fs.areIdentical(source, destination)) {
                return MigrateContentToStoreTaskResultEvent.FileMigration(null, MigrateStatus.Failed)
            }

            MigrateContentToStoreTaskResultEvent.FileMigration(destination.absolutePath, MigrateStatus.Completed)
        } catch (e: Exception) {
            MigrateContentToStoreTaskResultEvent.FileMigration(null, MigrateStatus.Failed)
        }
    }

    @VisibleForTesting
    internal fun migrateSubtitle(
        fs: FileSystemService,
        subtitleContents: List<MigrateToContentStoreTask.Data.SingleSubtitle>
    ): List<MigrateContentToStoreTaskResultEvent.SubtitleMigration> {
        if (subtitleContents.isEmpty()) return listOf(MigrateContentToStoreTaskResultEvent.SubtitleMigration(null,  null, MigrateStatus.NotPresent))
        val results = mutableListOf<MigrateContentToStoreTaskResultEvent.SubtitleMigration>()
        for (subtitle in subtitleContents) {
            val source = File(subtitle.cachedUri)
            val destination = File(subtitle.storeUri)
            try {
                if (!fs.copy(source, destination)) {
                    results.add(MigrateContentToStoreTaskResultEvent.SubtitleMigration(subtitle.language, destination.absolutePath, MigrateStatus.Failed))
                    continue
                }

                if (!fs.areIdentical(source, destination)) {
                    results.add(MigrateContentToStoreTaskResultEvent.SubtitleMigration(subtitle.language, destination.absolutePath, MigrateStatus.Failed))
                } else {
                    results.add(MigrateContentToStoreTaskResultEvent.SubtitleMigration(subtitle.language,destination.absolutePath, MigrateStatus.Completed))
                }
            } catch (e: Exception) {
                results.add(MigrateContentToStoreTaskResultEvent.SubtitleMigration(subtitle.language,destination.absolutePath, MigrateStatus.Failed))
            }
        }
        return results
    }

    @VisibleForTesting
    internal fun migrateCover(fs: FileSystemService, coverContents: List<MigrateToContentStoreTask.Data.SingleContent>): List<MigrateContentToStoreTaskResultEvent.FileMigration> {
        if (coverContents.isEmpty()) return listOf(MigrateContentToStoreTaskResultEvent.FileMigration(null, MigrateStatus.NotPresent))
        val results = mutableListOf<MigrateContentToStoreTaskResultEvent.FileMigration>()
        for (cover in coverContents) {
            val source = File(cover.cachedUri)
            val destination = File(cover.storeUri)
            try {
                if (!fs.copy(source, destination)) {
                    results.add(MigrateContentToStoreTaskResultEvent.FileMigration(destination.absolutePath, MigrateStatus.Failed))
                    continue
                }
                if (!fs.areIdentical(source, destination)) {
                    results.add(MigrateContentToStoreTaskResultEvent.FileMigration(destination.absolutePath, MigrateStatus.Failed))
                } else {
                    results.add(MigrateContentToStoreTaskResultEvent.FileMigration(destination.absolutePath, MigrateStatus.Completed))
                }
            } catch (e: Exception) {
                results.add(MigrateContentToStoreTaskResultEvent.FileMigration(destination.absolutePath, MigrateStatus.Failed))
            }
        }
        return results
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