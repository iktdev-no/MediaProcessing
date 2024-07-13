package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.contract.reader.OutputFilesDto

class OutputFilesMapping(val events: List<Event>) {

    /*fun mapTo(): OutputFilesDto {

        val videoResult = events.filter { it.data is ProcesserEncodeWorkPerformed }
            .map { it.data as ProcesserEncodeWorkPerformed }

        val subtitleResult = events.filter { it.data is ProcesserExtractWorkPerformed && it.isSuccess() }.map { it.data as ProcesserExtractWorkPerformed }.filter { !it.outFile.isNullOrBlank() }
        val convertedSubtitleResult = events.filter { it.data is ConvertWorkPerformed && it.isSuccess() }.map { it.data as ConvertWorkPerformed }

        val referenceId = events.firstOrNull()?.referenceId ?: throw RuntimeException("No Id")
        val subtitles = try {
            toSubtitleList(subtitleResult, convertedSubtitleResult)
        } catch (e: Exception) {
            System.err.println("Exception of $referenceId")
            System.err.print("EventIds:\n" + events.joinToString("\n") { it.eventId } + "\n")
            e.printStackTrace()
            throw e
        }

        return OutputFilesDto(
            video = videoResult.lastOrNull { it.isSuccess() }?.outFile,
            subtitles = subtitles
        )
    }

    private fun toSubtitleList(extracted: List<ProcesserExtractWorkPerformed>, converted: List<ConvertWorkPerformed>): List<String> {
        val sub1 = extracted.mapNotNull { it.outFile }
        val sub2 = converted.flatMap { it.outFiles }
        return sub1 + sub2
    }*/
}