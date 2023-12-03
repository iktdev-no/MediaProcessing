package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.reader.MediaProcessedDto

class ProcessMapping(val events: List<PersistentMessage>) {

    fun map(): MediaProcessedDto? {
        val referenceId = events.firstOrNull()?.referenceId ?: return null
        val processStarted = getProcessStarted()
        return MediaProcessedDto(
            referenceId = referenceId,
            process = processStarted?.type,
            inputFile = processStarted?.file,
            metadata = MetadataMapping(events).map(),
            outputFiles = null
        )
    }

    fun getProcessStarted(): ProcessStarted? {
        return events.lastOrNull { it.data is ProcessStarted }?.data as ProcessStarted?
    }

    fun waitsForEncode(): Boolean {
        val arguments = events.find { it.event == KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED.event } != null
        val performed = events.find { it.event == KafkaEvents.EVENT_WORK_ENCODE_PERFORMED.event } != null
        val isSkipped = events.find { it.event == KafkaEvents.EVENT_WORK_ENCODE_SKIPPED.event } != null
        return !(isSkipped || (arguments && performed))
    }

    fun waitsForExtract(): Boolean {
        val arguments = events.find { it.event == KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED.event } != null
        val performed = events.find { it.event == KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED.event } != null
        val isSkipped = events.find { it.event == KafkaEvents.EVENT_WORK_EXTRACT_SKIPPED.event } != null
        return !(isSkipped || (arguments && performed))
    }

    fun waitsForConvert(): Boolean {
        val arguments = events.find { it.event == KafkaEvents.EVENT_WORK_CONVERT_CREATED.event } != null
        val performed = events.find { it.event == KafkaEvents.EVENT_WORK_CONVERT_PERFORMED.event } != null
        val isSkipped = events.find { it.event == KafkaEvents.EVENT_WORK_CONVERT_SKIPPED.event } != null
        return !(isSkipped || (arguments && performed))
    }

    fun canCollect(): Boolean {
        return waitsForEncode() && waitsForExtract() && waitsForConvert()
    }

}