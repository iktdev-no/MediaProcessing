package no.iktdev.streamit.content.reader.analyzer

import no.iktdev.streamit.content.reader.Resources
import no.iktdev.streamit.library.kafka.KnownEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class EncodeStreamsMessageParserTest {
    val parser = EncodeStreamsMessageParser()
    val baseEvent = Message(status = Status( statusType = StatusType.SUCCESS))

    /*@Test
    fun getFileNameFromEvent() {
        val payload = Resources.Streams().getSample(3)
        assertDoesNotThrow {
            val msg = baseEvent.copy(data = payload)
            val result = parser.getMediaStreamsFromEvent(mutableListOf(
                Resources().getConsumerRecord(
                    KnownEvents.EVENT_READER_RECEIVED_STREAMS.event,
                    msg
                )
            ))
        }
    }

    @Test
    fun getMediaStreamsFromEvent() {
    }*/
}