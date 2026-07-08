package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks_super

import no.iktdev.eventi.models.Task
import no.iktdev.files.FileHash
import java.util.UUID

abstract class TransferTask(
    val executerId: UUID,
    val collection: String,
    val cachedUri: String,
    val storeUri: String,
    val cachedFileHash: FileHash? = null,
    var overrides: List<Overrides>? = emptyList()
): Task() {
    enum class Overrides {
        AllowOverwrite
    }
}