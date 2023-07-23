package no.iktdev.streamit.content.convert.kafka

import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.library.kafka.listener.SimpleMessageListener
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import org.springframework.stereotype.Service

@Service
class SubtitleConsumer: DefaultKafkaReader("convertHandler") {

    /*init {
        object: SimpleMessageListener(topic =b  )
    }*/

    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        TODO("Not yet implemented")
    }

}