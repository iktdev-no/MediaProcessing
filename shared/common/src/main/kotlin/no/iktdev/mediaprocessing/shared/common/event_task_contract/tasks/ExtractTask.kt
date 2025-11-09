package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task

data class ExtractTask(
    val data: ExtractData
): Task() {
}

data class ExtractData(
    val arguments: List<String>,
    val outputFileName: String,
    val language: String,
    val inputFile: String
) {

}



