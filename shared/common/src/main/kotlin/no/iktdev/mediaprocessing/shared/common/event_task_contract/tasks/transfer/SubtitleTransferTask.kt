package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.transfer

import no.iktdev.files.FileHash
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks_super.TransferTask
import java.util.UUID

class SubtitleTransferTask(
    executerId: UUID,
    collection: String,
    val language: String,
    cachedUri: String,
    storeUri: String,
    cachedFileHash: FileHash? = null,
    overrides: List<Overrides>? = emptyList()
): TransferTask(executerId = executerId, collection = collection, cachedUri = cachedUri, storeUri = storeUri, cachedFileHash = cachedFileHash, overrides = overrides) {
}