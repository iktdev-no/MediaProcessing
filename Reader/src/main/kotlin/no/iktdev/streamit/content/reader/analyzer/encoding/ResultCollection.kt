package no.iktdev.streamit.content.reader.analyzer.encoding

import no.iktdev.streamit.content.common.deserializers.ContentOutNameDeserializer
import no.iktdev.streamit.content.common.deserializers.FileResultDeserializer
import no.iktdev.streamit.content.common.deserializers.MediaStreamsDeserializer
import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.common.streams.MediaStreams
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.collector.DefaultEventCollection
import no.iktdev.streamit.library.kafka.listener.deserializer.deserializeIfSuccessful
import org.apache.kafka.clients.consumer.ConsumerRecord

class ResultCollection: DefaultEventCollection() {

    fun getFirstOrNull(events: KafkaEvents): ConsumerRecord<String, Message>? {
        return getRecords().firstOrNull { it.key() == events.event }
    }

}