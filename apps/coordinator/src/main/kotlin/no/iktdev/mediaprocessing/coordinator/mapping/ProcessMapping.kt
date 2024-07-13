package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.contract.reader.MediaProcessedDto

class ProcessMapping(val events: List<Event>) {

   /* fun map(): MediaProcessedDto? {
        val referenceId = events.firstOrNull()?.referenceId ?: return null
        val processStarted = getProcessStarted()
        val meta = MetadataMapping(events)
        return MediaProcessedDto(
            referenceId = referenceId,
            process = processStarted?.type,
            inputFile = processStarted?.file,
            collection = meta.collection,
            metadata = meta.map(),
            videoDetails = VideoDetailsMapper(events).mapTo(),
            outputFiles = OutputFilesMapping(events).mapTo()
        )
    }

    fun getProcessStarted(): MediaProcessStarted? {
        return events.lastOrNull { it.data is MediaProcessStarted }?.data as MediaProcessStarted?
    }


    fun waitsForEncode(): Boolean {
        val arguments = events.filter { it.event == KafkaEvents.EventMediaParameterEncodeCreated }
        val created = events.filter { it.event == KafkaEvents.EventWorkEncodeCreated}

        val performedEvents = events.filter { it.event == KafkaEvents.EventWorkEncodePerformed }

        val performed = performedEvents.filter { it.isSuccess() }
        val isSkipped = performedEvents.filter { it.isSkipped() }

        return (arguments.isNotEmpty() && created.isEmpty()) || created.size > performed.size + isSkipped.size
    }

    fun waitsForExtract(): Boolean {
        // Check if message is declared as skipped with statis
        val arguments = events.filter { it.event == KafkaEvents.EventMediaParameterExtractCreated }.filter { it.data.isSuccess() }
        val created = events.filter { it.event == KafkaEvents.EventWorkExtractCreated }

        val performedEvents = events.filter { it.event == KafkaEvents.EventWorkExtractPerformed }

        val performed = performedEvents.filter { it.isSuccess() }
        val isSkipped = performedEvents.filter { it.isSkipped() }


        return (arguments.isNotEmpty() && created.isEmpty()) || created.size > performed.size + isSkipped.size
    }

    fun waitsForConvert(): Boolean {
        val created = events.filter { it.event == KafkaEvents.EventWorkConvertCreated }
        val performedEvents = events.filter { it.event == KafkaEvents.EventWorkConvertPerformed }

        val performed = performedEvents.filter { it.isSuccess() }
        val isSkipped = performedEvents.filter { it.isSkipped() }

        return created.size > performed.size + isSkipped.size
    }

    fun canCollect(): Boolean {
        return (!waitsForEncode() && !waitsForExtract() && !waitsForConvert())
    }*/

}