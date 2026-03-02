package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class ExtractSubtitleTask(
    val data: ExtractSubtitleData
): Task() {
}

data class ExtractSubtitleData(
    val inputFile: String,
    val arguments: List<String>,
    val outputFileName: String,
    val outputFolderName: String? = null,
    val language: String,
) {

}



