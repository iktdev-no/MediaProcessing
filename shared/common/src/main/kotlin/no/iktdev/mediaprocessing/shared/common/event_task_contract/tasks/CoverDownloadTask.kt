package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class CoverDownloadTask(
    val data: CoverDownloadData
): Task() {
}

data class CoverDownloadData(val url: String, val outputFile: String)