package no.iktdev.streamit.content.ui.kafka.converter

import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.content.ui.dto.EventDataObject
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import org.springframework.stereotype.Component

@Component
class EventDataFilenameAndTypeDeterminerSubConverter : EventDataSubConverterBase() {
    override fun convertEvents(eventData: EventDataObject, events: Map<String, Message>) {

        val convertedFileNameEvent = events.entries
            .asSequence()
            .filter { it.key == KafkaEvents.EVENT_READER_DETERMINED_FILENAME.event }
            .filter { it.value.isSuccessful() }
            .map { DeserializerRegistry.getDeserializerForEvent(it.key)?.deserialize(it.value) }
            .filterIsInstance<ContentOutName>()
            .lastOrNull() ?: return

        val convertedType = events.entries
            .asSequence()
            .filter { it -> listOf(KafkaEvents.EVENT_READER_DETERMINED_SERIE.event, KafkaEvents.EVENT_READER_DETERMINED_MOVIE.event).contains(it.key) }
            .lastOrNull()
            ?.toPair()
        val type = when(convertedType?.first) {
            KafkaEvents.EVENT_READER_DETERMINED_SERIE.event -> "serie"
            KafkaEvents.EVENT_READER_DETERMINED_MOVIE.event -> "movie"
            else -> null
        }

        eventData.details = eventData.details?.apply {
            this.title = convertedFileNameEvent.baseName
            this.type = type
        }
    }


}