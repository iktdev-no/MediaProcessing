package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.shared.common.model.SubtitleFormat

data class ConvertTask(
    val data: Data
): Task() {
    data class Data(
        val inputFile: String,
        val language: String,
        val outputDirectory: String,
        val outputFileName: String,
        val formats: List<SubtitleFormat> = emptyList(),
        val allowOverwrite: Boolean
    )
}

