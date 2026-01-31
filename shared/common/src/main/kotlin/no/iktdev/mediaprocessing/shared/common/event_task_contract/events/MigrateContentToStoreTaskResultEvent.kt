package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.TaskResultEvent
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus

class MigrateContentToStoreTaskResultEvent(
    val migrateData: MigrateData? = null,
    status: TaskStatus,
    error: String? = null
) : TaskResultEvent(status, error) {

    data class MigrateData(
        val collection: String,
        val videoMigrate: FileMigration,
        val subtitleMigrate: List<SubtitleMigration>,
        val coverMigrate: List<FileMigration>,
    )

    data class FileMigration(
        val storedUri: String?,
        val status: MigrateStatus
    )

    data class SubtitleMigration(
        val language: String?,
        val storedUri: String?,
        val status: MigrateStatus
    ) {
        init {
            if (status == MigrateStatus.Completed && language == null)
                throw IllegalStateException("SubtitleMigration: language cannot be null when status is COMPLETED")

        }
    }
}