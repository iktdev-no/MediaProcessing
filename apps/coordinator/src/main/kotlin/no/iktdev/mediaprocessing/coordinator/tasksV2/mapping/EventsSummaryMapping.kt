package no.iktdev.mediaprocessing.coordinator.tasksV2.mapping

import no.iktdev.eventi.data.dataAs
import no.iktdev.eventi.data.isSuccessful
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.EventSummary
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationsSummary
import no.iktdev.mediaprocessing.shared.common.contract.dto.OutputFiles
import no.iktdev.mediaprocessing.shared.common.getChecksum

class EventsSummaryMapping {

    fun map(events: List<Event>): EventSummary {
        val startOperations = events.find { it.eventType == Events.EventMediaProcessStarted }?.dataAs<StartEventData>() ?: throw RuntimeException("No start event found")
        val successOperations = listOfNotNull(
            if (isEncodedSuccessful(events)) OperationEvents.ENCODE else null,
            if (isExtractedSuccessful(events)) OperationEvents.EXTRACT else null,
            if (isConvertedSuccessful(events)) OperationEvents.CONVERT else null
        )


        return EventSummary(
            inputFile = startOperations.file,
            inputFileChecksum = getChecksum(startOperations.file),
            operationsSummary = OperationsSummary(
                requestedOperations = startOperations.operations,
                completedOperations = successOperations
            ),
            outputFiles = getProducesFiles(events)
        )
    }


    fun isEncodedSuccessful(events: List<Event>) = events.filter { it.eventType == Events.EventWorkEncodePerformed }.any { it.isSuccessful() }
    fun isExtractedSuccessful(events: List<Event>) = events.filter { it.eventType == Events.EventWorkExtractPerformed }.any { it.isSuccessful() }
    fun isConvertedSuccessful(events: List<Event>) = events.filter { it.eventType == Events.EventWorkConvertPerformed }.any { it.isSuccessful() }

    fun getProducesFiles(events: List<Event>): OutputFiles {
        val encoded = if (isEncodedSuccessful(events)) {
            events.filter { it.eventType == Events.EventWorkEncodePerformed }
                .filter { it.isSuccessful() }
                .mapNotNull { it.dataAs<EncodedData>()?.outputFile }
        } else emptyList()

        val extracted = if (isExtractedSuccessful(events)) {
            events.filter { it.eventType == Events.EventWorkExtractPerformed }
                .filter { it.isSuccessful() }
                .mapNotNull { it.dataAs<ExtractedData>() }
                .map { it.outputFile }
        } else emptyList()

        val converted = if (isConvertedSuccessful(events)) {
            events.filter { it.eventType == Events.EventWorkConvertPerformed }
                .filter { it.isSuccessful() }
                .mapNotNull { it.dataAs<ConvertedData>() }
                .flatMap { it.outputFiles }
        } else emptyList()

        return OutputFiles(
            encoded = encoded,
            extracted = extracted,
            converted = converted
        )
    }

}