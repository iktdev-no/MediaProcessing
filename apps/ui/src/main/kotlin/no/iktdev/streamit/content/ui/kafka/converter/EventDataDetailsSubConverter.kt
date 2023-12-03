package no.iktdev.streamit.content.ui.kafka.converter

import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.ui.dto.Details
import no.iktdev.streamit.content.ui.dto.EventDataObject
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import org.springframework.stereotype.Component

@Component
class EventDataDetailsSubConverter : EventDataSubConverterBase() {
    override fun convertEvents(eventData: EventDataObject, events: Map<String, Message>) {
        val event = events.entries
            .asSequence()
            .filter { it.key == KafkaEvents.EVENT_READER_RECEIVED_FILE.event }
            .filter { it.value.isSuccessful() }
            .map { DeserializerRegistry.getDeserializerForEvent(it.key)?.deserialize(it.value) }
            .filterIsInstance<FileResult>()
            .lastOrNull() ?: return

        val deserialized = Details(
            name = event.title,
            file = event.file,
            sanitizedName = event.sanitizedName
        )
        eventData.details = deserialized
    }


}