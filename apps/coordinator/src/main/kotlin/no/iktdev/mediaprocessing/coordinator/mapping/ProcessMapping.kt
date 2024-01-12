package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.contract.reader.MediaProcessedDto

class ProcessMapping(val events: List<PersistentMessage>) {

    fun map(): MediaProcessedDto? {
        val referenceId = events.firstOrNull()?.referenceId ?: return null
        val processStarted = getProcessStarted()
        val meta = MetadataMapping(events)
        return MediaProcessedDto(
            referenceId = referenceId,
            process = processStarted?.type,
            inputFile = processStarted?.file,
            collection = meta.getCollection(),
            metadata = meta.map(),
            videoDetails = VideoDetailsMapper(events).mapTo(),
            outputFiles = OutputFilesMapping(events).mapTo()
        )
    }

    fun getProcessStarted(): ProcessStarted? {
        return events.lastOrNull { it.data is ProcessStarted }?.data as ProcessStarted?
    }


    fun waitsForEncode(): Boolean {
        val arguments = events.filter { it.event == KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED }
        val created = events.filter { it.event == KafkaEvents.EVENT_WORK_ENCODE_CREATED}

        val performed = events.filter { it.event == KafkaEvents.EVENT_WORK_ENCODE_PERFORMED }
        val isSkipped = events.filter { it.event == KafkaEvents.EVENT_WORK_ENCODE_SKIPPED }

        return (arguments.isNotEmpty() && created.isEmpty()) || created.size > performed.size + isSkipped.size
    }

    fun waitsForExtract(): Boolean {
        val arguments = events.filter { it.event == KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED }
        val created = events.filter { it.event == KafkaEvents.EVENT_WORK_EXTRACT_CREATED }

        val performed = events.filter { it.event == KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED }
        val isSkipped = events.filter { it.event == KafkaEvents.EVENT_WORK_EXTRACT_SKIPPED }

        return (arguments.isNotEmpty() && created.isEmpty()) || created.size > performed.size + isSkipped.size
    }

    fun waitsForConvert(): Boolean {
        val created = events.filter { it.event == KafkaEvents.EVENT_WORK_CONVERT_CREATED }
        val performed = events.filter { it.event == KafkaEvents.EVENT_WORK_CONVERT_PERFORMED }
        val isSkipped = events.filter { it.event == KafkaEvents.EVENT_WORK_CONVERT_SKIPPED }

        return created.size > performed.size + isSkipped.size
    }

    fun canCollect(): Boolean {
        return (!waitsForEncode() && !waitsForExtract() && !waitsForConvert())
    }

}