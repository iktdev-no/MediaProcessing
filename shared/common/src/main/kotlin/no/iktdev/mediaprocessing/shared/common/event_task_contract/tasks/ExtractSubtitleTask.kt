package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.shared.common.event_task_contract.Overrides

data class ExtractSubtitleTask(
    val data: ExtractSubtitleData,
    val overrides: List<Overrides>? = null
): Task() {
}

data class ExtractSubtitleData(
    val inputFile: String,
    val instructions: FFmpegInstructions,
    val outputFileName: String,
    val outputFolderName: String,
    val language: String,
) {

}



