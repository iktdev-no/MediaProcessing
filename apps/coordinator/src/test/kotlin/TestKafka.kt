import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.common.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultConsumer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

class TestKafka {
    companion object {
        private var listen: Boolean = false
        private val topic = "nan"
        private val gson = Gson()

        val consumer = object : DefaultConsumer() {
            override fun consumerFactory(): DefaultKafkaConsumerFactory<String, String> {
                val config: MutableMap<String, Any> = HashMap()
                config[ConsumerConfig.GROUP_ID_CONFIG] = "${KafkaEnv.consumerId}:$subId"
                config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
                config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = autoCommit
                return DefaultKafkaConsumerFactory(config, StringDeserializer(), StringDeserializer())
            }
        }

        val listener = object: DefaultMessageListener(topic, consumer) {
            override fun listen() {
                listen = true
            }
        }

        val producer = object: CoordinatorProducer() {

            val messages = mutableListOf<ConsumerRecord<String, String>>()

            override fun usingKafkaTemplate(): KafkaTemplate<String, String> {
                val producerFactory: ProducerFactory<String, String> = DefaultKafkaProducerFactory(mapOf(
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java
                ))
                return KafkaTemplate(producerFactory)
            }


            override fun sendMessage(key: String, message: Message<MessageDataWrapper>) {
                val mockRecord = ConsumerRecord(
                    topic,
                    0,
                    messages.size.toLong(),
                    key,
                    gson.toJson(message)
                )
                if (listen) {
                    messages.add(mockRecord)
                    listener.onMessage(mockRecord)
                }
            }
        }
    }


}