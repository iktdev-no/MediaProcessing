package no.iktdev.streamit.content.ui.kafka.converter

import no.iktdev.streamit.content.ui.dto.EventDataObject
import no.iktdev.streamit.library.kafka.dto.Message
import org.springframework.stereotype.Component


@Component
class EventDataMetadataSubConverter: EventDataSubConverterBase() {
    override fun convertEvents(eventData: EventDataObject, events: Map<String, Message>) {

    }

}