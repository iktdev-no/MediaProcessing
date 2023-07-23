package no.iktdev.streamit.content.encode

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.deserializers.ExtractWorkDeserializer
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.content.encode.runner.RunnerCoordinator
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.SimpleMessageListener
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import no.iktdev.streamit.library.kafka.listener.deserializer.deserializeIfSuccessful
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
private val logger = KotlinLogging.logger {}

@Service
class ExtractWorkConsumer(private val runnerCoordinator: RunnerCoordinator) : DefaultKafkaReader("extractWork") {
    lateinit var encodeInstructionsListener: ExtractWorkListener

    init {
        encodeInstructionsListener = ExtractWorkListener(
            topic = CommonConfig.kafkaTopic,
            defaultConsumer,
            accepts = listOf(KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE.event),
            runnerCoordinator
        )
        encodeInstructionsListener.listen()
    }

    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(
            KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE
        )
    }


    class ExtractWorkListener(
        topic: String,
        consumer: DefaultConsumer,
        accepts: List<String>,
        val runnerCoordinator: RunnerCoordinator
    ) : SimpleMessageListener(
        topic, consumer,
        accepts
    ) {
        override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
            logger.info { "${data.value().referenceId}: ${data.key()} ${Gson().toJson(data.value())}" }
            val message = data.value().apply {
                this.data = ExtractWorkDeserializer().deserializeIfSuccessful(data.value())
            }
            runnerCoordinator.addExtractMessageToQueue(message)
        }
    }
}