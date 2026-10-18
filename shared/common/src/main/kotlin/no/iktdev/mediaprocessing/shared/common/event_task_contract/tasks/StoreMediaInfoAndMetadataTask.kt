package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models.MediaImportV2
import no.iktdev.mediaprocessing.shared.common.model.ContentExport

data class StoreMediaInfoAndMetadataTask(
    val data: MediaImportV2
): Task() {
}