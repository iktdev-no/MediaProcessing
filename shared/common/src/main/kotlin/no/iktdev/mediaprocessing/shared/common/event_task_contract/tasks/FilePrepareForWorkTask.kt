package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class FilePrepareForWorkTask(
    val data: Data
): Task() {
    data class Data(
        val sourceFile: String,
        val destinationFile: String
    )
}