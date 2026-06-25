package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.model.ContentExport

data class StoreMediaInfoAndMetadataTask(
    val data: ContentExport
): Task() {
}