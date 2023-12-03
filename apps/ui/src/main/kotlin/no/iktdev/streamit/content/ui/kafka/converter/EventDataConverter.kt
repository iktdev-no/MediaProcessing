package no.iktdev.streamit.content.ui.kafka.converter

import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.streamit.content.ui.dto.EventDataObject
import no.iktdev.streamit.content.ui.memActiveEventMap
import no.iktdev.streamit.content.ui.kafka.EventConsumer
import no.iktdev.streamit.content.ui.memSimpleConvertedEventsMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EventDataConverter {

    @Autowired private lateinit var detailsConverter: EventDataDetailsSubConverter
    @Autowired private lateinit var encodeConverter: EventDataEncodeSubConverter
    @Autowired private lateinit var metadataConverter: EventDataMetadataSubConverter
    @Autowired private lateinit var fileNameConverter: EventDataFilenameAndTypeDeterminerSubConverter

    fun convertEventToObject(eventReferenceId: String) {
        val data = memActiveEventMap[eventReferenceId] ?: EventDataObject(id = eventReferenceId)
        val collection = EventConsumer.idAndEvents[eventReferenceId] ?: emptyMap<String, Message>()

        detailsConverter.convertAndUpdate(data, collection.toMap())
        encodeConverter.convertAndUpdate(data, collection.toMap())
        metadataConverter.convertAndUpdate(data, collection.toMap())
        fileNameConverter.convertAndUpdate(data, collection.toMap())


        memActiveEventMap[eventReferenceId] = data
        memSimpleConvertedEventsMap[eventReferenceId] = data.toSimple()
    }

}