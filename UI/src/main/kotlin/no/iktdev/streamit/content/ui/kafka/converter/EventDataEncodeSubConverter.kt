package no.iktdev.streamit.content.ui.kafka.converter

import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.State
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.ui.dto.Encode
import no.iktdev.streamit.content.ui.dto.EventDataObject
import no.iktdev.streamit.content.ui.dto.IO
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import org.springframework.stereotype.Component

@Component
class EventDataEncodeSubConverter : EventDataSubConverterBase() {
    override fun convertEvents(eventData: EventDataObject, events: Map<String, Message>) {
        val filteredEvents = events.entries
            .asSequence()
            .filter {
                listOf(
                    KafkaEvents.EVENT_ENCODER_VIDEO_FILE_QUEUED.event,
                    KafkaEvents.EVENT_ENCODER_VIDEO_FILE_STARTED.event,
                    KafkaEvents.EVENT_ENCODER_VIDEO_FILE_ENDED.event
                ).contains(it.key)
            }

        val event = filteredEvents
            .map { DeserializerRegistry.getDeserializerForEvent(it.key)?.deserialize(it.value) }
            .filterIsInstance<EncodeWork>()
            .lastOrNull() ?: return

        event.let {
            eventData.details?.apply { this.collection = it.collection }
        }

        eventData.io = IO(event.inFile, event.outFile)
        eventData.encode =
            if (eventData.encode != null)
                eventData.encode?.apply { state = getState(filteredEvents).name }
            else
                Encode(state = getState(filteredEvents).name)

    }

    private fun getState(events: Sequence<Map.Entry<String, Message>>): State {
        val last = events.lastOrNull()
            ?: return State.QUEUED
        if (!last.value.isSuccessful()) return State.FAILURE
        return when (last.key) {
            KafkaEvents.EVENT_ENCODER_VIDEO_FILE_STARTED.event -> State.STARTED
            KafkaEvents.EVENT_ENCODER_VIDEO_FILE_ENDED.event -> State.ENDED
            else -> State.QUEUED
        }
    }

}

