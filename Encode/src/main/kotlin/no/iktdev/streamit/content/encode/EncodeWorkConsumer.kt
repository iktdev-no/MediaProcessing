package no.iktdev.streamit.content.encode

import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.deserializers.EncodeWorkDeserializer
import no.iktdev.streamit.content.encode.runner.RunnerCoordinator
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.SimpleMessageListener
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import no.iktdev.streamit.library.kafka.listener.deserializer.deserializeIfSuccessful
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class EncodeWorkConsumer(private val runnerCoordinator: RunnerCoordinator) : DefaultKafkaReader("encodeWork") {
    lateinit var encodeInstructionsListener: EncodeInformationListener

    init {
        encodeInstructionsListener = EncodeInformationListener(
            topic = CommonConfig.kafkaTopic,
            defaultConsumer,
            accepts = listOf(KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO.event),
            runnerCoordinator
        )
        encodeInstructionsListener.listen()
    }

    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(
            KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO
        )
    }


    class EncodeInformationListener(
        topic: String,
        consumer: DefaultConsumer,
        accepts: List<String>,
        val runnerCoordinator: RunnerCoordinator
    ) : SimpleMessageListener(
        topic, consumer,
        accepts
    ) {
        override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
            val message = data.value().apply {
                this.data = EncodeWorkDeserializer().deserializeIfSuccessful(data.value())
            }
            runnerCoordinator.addEncodeMessageToQueue(message)
        }
    }
}