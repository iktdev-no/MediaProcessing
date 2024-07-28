package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping

import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams.SubtitleArguments
import no.iktdev.mediaprocessing.shared.common.contract.data.ExtractArgumentData
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams
import java.io.File

class ExtractWorkArgumentsMapping(
    val inputFile: String,
    val outFileFullName: String,
    val outFileAbsolutePathFile: File,
    val streams: ParsedMediaStreams
) {

    fun getArguments(): List<ExtractArgumentData> {
        val subDir = outFileAbsolutePathFile.using("sub")
        val sArg = SubtitleArguments(streams.subtitleStream).getSubtitleArguments()

        val entries = sArg.map {
            ExtractArgumentData(
                inputFile = inputFile,
                arguments = it.codecParameters + it.optionalParameters + listOf("-map", "0:s:${it.index}"),
                outputFile = subDir.using(it.language, "${outFileFullName}.${it.format}").absolutePath
            )
        }

        return entries
    }

}