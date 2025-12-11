package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class ExtractSubtitleTask(
    val data: ExtractSubtitleData
): Task() {
}

data class ExtractSubtitleData(
    val arguments: List<String>,
    val outputFileName: String,
    val language: String,
    val inputFile: String
) {

}



