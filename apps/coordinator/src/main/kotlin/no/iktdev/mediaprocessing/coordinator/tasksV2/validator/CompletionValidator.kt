package no.iktdev.mediaprocessing.coordinator.tasksV2.validator

import no.iktdev.eventi.data.dataAs
import no.iktdev.eventi.data.isSkipped
import no.iktdev.eventi.data.isSuccessful
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import no.iktdev.mediaprocessing.shared.common.contract.dto.SubtitleFormats
import java.io.File

/**
 * Validates whether all the required work has been created and processed in accordance with expected behaviour and sequence
 */
object CompletionValidator {

    /**
     * Checks whether it requires encode or extract or both, and it has created events with args
     */
    fun req1(started: MediaProcessStartEvent, events: List<Event>): Boolean {
        val encodeFulfilledOrSkipped = if (started.data?.operations?.contains(OperationEvents.ENCODE) == true) {
            events.any { it.eventType == Events.EncodeParameterCreated }
        } else true

        val extractFulfilledOrSkipped = if (started.data?.operations?.contains(OperationEvents.EXTRACT) == true) {
            events.any { it.eventType == Events.ExtractParameterCreated }
        } else true

        if (!encodeFulfilledOrSkipped || !extractFulfilledOrSkipped) {
            return false
        } else return true
    }

    /**
     * Checks whether work that was supposed to be created has been created.
     * Checks if all subtitles that can be processed has been created if convert is set.
     */
    fun req2(operations: List<OperationEvents>, events: List<Event>): Boolean {
        if (OperationEvents.ENCODE in operations) {
            val encodeParamter = events.find { it.eventType == Events.EncodeParameterCreated }?.az<EncodeArgumentCreatedEvent>()
            val encodeWork = events.find { it.eventType == Events.EncodeTaskCreated }
            if (encodeParamter?.isSuccessful() == true && (encodeWork == null))
                return false
        }

        val extractParamter = events.find { it.eventType == Events.ExtractParameterCreated }?.az<ExtractArgumentCreatedEvent>()
        val extractWork = events.filter { it.eventType == Events.ExtractTaskCreated }
        if (OperationEvents.EXTRACT in operations) {
            if (extractParamter?.isSuccessful() == true && extractParamter.data?.size != extractWork.size)
                return false
        }

        if (OperationEvents.CONVERT in operations) {
            val convertWork = events.filter { it.eventType == Events.ConvertTaskCreated }

            val supportedSubtitleFormats = SubtitleFormats.entries.map { it.name }
            val eventsSupportsConvert = extractWork.filter { it.data is ExtractArgumentData }
                .filter { (it.dataAs<ExtractArgumentData>()?.outputFileName?.let { f -> File(f).extension.uppercase() } in supportedSubtitleFormats) }

            if (convertWork.size != eventsSupportsConvert.size)
                return false
        }

        return true
    }

    /**
     * Checks whether all work that has been created has been completed
     */
    fun req3(operations: List<OperationEvents>, events: List<Event>): Boolean {
        if (OperationEvents.ENCODE in operations) {
            val encodeWork = events.filter { it.eventType == Events.EncodeTaskCreated }
            val encodePerformed = events.filter { it.eventType == Events.EncodeTaskCompleted }
            if (encodePerformed.size < encodeWork.size)
                return false
        }

        if (OperationEvents.EXTRACT in operations) {
            val extractWork = events.filter { it.eventType == Events.ExtractTaskCreated }
            val extractPerformed = events.filter { it.eventType == Events.ExtractTaskCompleted }
            if (extractPerformed.size < extractWork.size)
                return false
        }

        if (OperationEvents.CONVERT in operations) {
            val convertWork = events.filter { it.eventType == Events.ConvertTaskCreated }
            val convertPerformed = events.filter { it.eventType == Events.ConvertTaskCompleted }
            if (convertPerformed.size < convertWork.size)
                return false
        }

        return true
    }

    /**
     * Checks if metadata has cover, if so, 2 events are expected
     */
    fun req4(events: List<Event>): Boolean {
        val metadata = events.find { it.eventType == Events.MetadataSearchPerformed }
        if (metadata?.isSkipped() == true) {
            return true
        }

        if (metadata?.isSuccessful() != true) {
            return true
        }

        val hasCover = metadata.dataAs<pyMetadata>()?.cover != null
        if (hasCover == false) {
            return true
        }

        if (events.any { it.eventType == Events.ReadOutCover } && events.any { it.eventType == Events.CoverDownloaded }) {
            return true
        }
        return false
    }
}