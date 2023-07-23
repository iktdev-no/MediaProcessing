package no.iktdev.streamit.content.reader.analyzer.encoding

import no.iktdev.streamit.content.common.deserializers.ContentOutNameDeserializer
import no.iktdev.streamit.content.common.deserializers.FileResultDeserializer
import no.iktdev.streamit.content.common.deserializers.MediaStreamsDeserializer
import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.common.streams.MediaStreams
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.collector.EventCollection
import no.iktdev.streamit.library.kafka.listener.deserializer.deserializeIfSuccessful
import org.apache.kafka.clients.consumer.ConsumerRecord

class ResultCollection: EventCollection() {

    fun getFirstOrNull(events: KafkaEvents): ConsumerRecord<String, Message>? {
        return getRecords().firstOrNull { it.key() == events.event }
    }
    fun getFileResult(): FileResult? {
        val record = getRecords().firstOrNull { it.key() == KafkaEvents.EVENT_READER_RECEIVED_FILE.event } ?: return null
        return FileResultDeserializer().deserializeIfSuccessful(record.value())
    }

    fun getFileName(): ContentOutName? {
        val record = getFirstOrNull(KafkaEvents.EVENT_READER_DETERMINED_FILENAME) ?: return null
        return ContentOutNameDeserializer().deserializeIfSuccessful(record.value())
    }

    fun getStreams(): MediaStreams? {
        val record = getFirstOrNull(KafkaEvents.EVENT_READER_RECEIVED_STREAMS) ?: return null
        return MediaStreamsDeserializer().deserializeIfSuccessful(record.value())
    }
}