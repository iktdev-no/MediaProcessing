package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.reader.OutputFilesDto
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserEncodeWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.work.ProcesserExtractWorkPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess

class OutputFilesMapping(val events: List<PersistentMessage>) {

    fun mapTo(): OutputFilesDto {

        val videoResult = events.filter { it.data is ProcesserEncodeWorkPerformed }
            .map { it.data as ProcesserEncodeWorkPerformed }

        val subtitleResult = events.filter { it.data is ProcesserExtractWorkPerformed && it.data.isSuccess() }.map { it.data as ProcesserExtractWorkPerformed }
        val convertedSubtitleResult = events.filter { it.data is ConvertWorkPerformed && it.data.isSuccess() }.map { it.data as ConvertWorkPerformed }



        return OutputFilesDto(
            video = videoResult.lastOrNull { it.isSuccess() }?.outFile,
            subtitles = toSubtitleList(subtitleResult, convertedSubtitleResult)
        )
    }

    private fun toSubtitleList(extracted: List<ProcesserExtractWorkPerformed>, converted: List<ConvertWorkPerformed>): List<String> {
        val sub1 = extracted.mapNotNull { it.outFile }
        val sub2 = converted.flatMap { it.outFiles }
        return sub1 + sub2
    }
}