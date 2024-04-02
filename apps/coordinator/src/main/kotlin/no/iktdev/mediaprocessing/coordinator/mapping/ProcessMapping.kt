package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.isSkipped
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.contract.reader.MediaProcessedDto
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess

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

    fun getProcessStarted(): MediaProcessStarted? {
        return events.lastOrNull { it.data is MediaProcessStarted }?.data as MediaProcessStarted?
    }


    fun waitsForEncode(): Boolean {
        val arguments = events.filter { it.event == KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED }
        val created = events.filter { it.event == KafkaEvents.EVENT_WORK_ENCODE_CREATED}

        val performed = events.filter { it.event == KafkaEvents.EVENT_WORK_ENCODE_PERFORMED }
        val isSkipped = events.filter { it.isSkipped() }

        return (arguments.isNotEmpty() && created.isEmpty()) || created.size > performed.size + isSkipped.size
    }

    fun waitsForExtract(): Boolean {
        // Check if message is declared as skipped with statis
        val arguments = events.filter { it.event == KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED }.filter { it.data.isSuccess() }
        val created = events.filter { it.event == KafkaEvents.EVENT_WORK_EXTRACT_CREATED }

        val performed = events.filter { it.event == KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED }
        val isSkipped = events.filter { it.isSkipped() }

        return (arguments.isNotEmpty() && created.isEmpty()) || created.size > performed.size + isSkipped.size
    }

    fun waitsForConvert(): Boolean {
        val created = events.filter { it.event == KafkaEvents.EVENT_WORK_CONVERT_CREATED }
        val performed = events.filter { it.event == KafkaEvents.EVENT_WORK_CONVERT_PERFORMED }
        val isSkipped = events.filter { it.isSkipped() }

        return created.size > performed.size + isSkipped.size
    }

    fun canCollect(): Boolean {
        return (!waitsForEncode() && !waitsForExtract() && !waitsForConvert())
    }

}