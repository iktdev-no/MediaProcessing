package no.iktdev.streamit.content.ui.kafka.converter

import no.iktdev.streamit.content.ui.dto.EventDataObject
import no.iktdev.streamit.library.kafka.dto.Message

abstract class EventDataSubConverterBase {

    protected abstract fun convertEvents(eventData: EventDataObject, events: Map<String, Message>)

    fun convertAndUpdate(eventData: EventDataObject, events: Map<String, Message>) {
        try {
            convertEvents(eventData, events)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}