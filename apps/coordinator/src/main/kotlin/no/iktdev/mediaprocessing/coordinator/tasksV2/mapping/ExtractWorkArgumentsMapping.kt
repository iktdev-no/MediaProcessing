package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping

import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.streams.SubtitleArguments
import no.iktdev.mediaprocessing.shared.common.contract.data.ExtractArgumentData
import no.iktdev.mediaprocessing.shared.common.contract.ffmpeg.ParsedMediaStreams
import java.io.File

class ExtractWorkArgumentsMapping(
    val inputFile: String,
    val outFileFullName: String,
    val streams: ParsedMediaStreams
) {

    fun getArguments(): List<ExtractArgumentData> {
        val sArg = SubtitleArguments(streams.subtitleStream).getSubtitleArguments()

        val entries = sArg.map {
            ExtractArgumentData(
                inputFile = inputFile,
                language = it.language,
                arguments = it.codecParameters + it.optionalParameters + listOf("-map", "0:s:${it.index}"),
                outputFileName = "${outFileFullName}.${it.language}.${it.format}",
                storeFileName = outFileFullName
            )
        }

        return entries
    }

}