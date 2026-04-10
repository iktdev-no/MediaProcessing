package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan

data class MigrateToContentStoreTask(
    val data: ContentMigrationPlan,
    var overrides: List<Overrides>? = emptyList()
): Task() {
    enum class Overrides {
        AllowOverwrite
    }
}