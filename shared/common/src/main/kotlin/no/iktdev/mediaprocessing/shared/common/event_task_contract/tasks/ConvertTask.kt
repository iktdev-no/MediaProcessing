package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class ConvertTask(
    val data: Data
): Task() {
}

data class Data(
    val inputFile: String,
    val language: String,
    val outputDirectory: String,
    val outputFileName: String,
    val storeFileName: String,
    val formats: List<SubtitleFormats> = emptyList(),
    val allowOverwrite: Boolean
)


enum class SubtitleFormats {
    ASS,
    SRT,
    VTT,
    SMI
}