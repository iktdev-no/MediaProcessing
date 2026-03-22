package no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks

import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions

data class ExtractSubtitleTask(
    val data: ExtractSubtitleData
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



