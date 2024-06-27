package no.iktdev.mediaprocessing.shared.common.task

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import no.iktdev.mediaprocessing.shared.common.persistance.tasks
import no.iktdev.mediaprocessing.shared.contract.dto.SubtitleFormats
import org.jetbrains.exposed.sql.ResultRow
import java.io.Serializable

open class TaskData(
    val inputFile: String,
): Serializable {
}


class FfmpegTaskData(
    inputFile: String,
    val arguments: List<String>,
    val outFile: String
): TaskData(inputFile = inputFile)

class ConvertTaskData(
    inputFile: String,
    val allowOverwrite: Boolean,
    val outFileBaseName: String,
    val outDirectory: String,
    val outFormats: List<SubtitleFormats> = listOf()
): TaskData(inputFile = inputFile)