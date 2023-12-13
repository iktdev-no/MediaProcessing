package no.iktdev.mediaprocessing.shared.kafka.core

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import org.springframework.stereotype.Component
import java.lang.IllegalArgumentException
import java.util.*

@Component
open class DefaultMessageListener(
) : MessageListener<String, String> {

    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var consumerFactory: ConsumerFactory<String, String>

    var onMessageReceived: (DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) -> Unit = {
        logger.warn { "onMessageReceived has no listener" }
    }

    private val deserializer = DeserializingRegistry()

    protected var container: KafkaMessageListenerContainer<String, String>? = null

    open fun listen(topic: String) {
        val listener = ConcurrentKafkaListenerContainerFactory<String,String>().apply {
            consumerFactory = this@DefaultMessageListener.consumerFactory
        }
        val containerProperties = ContainerProperties(topic).apply {
            messageListener = this@DefaultMessageListener
        }
        container = KafkaMessageListenerContainer(listener.consumerFactory, containerProperties)
        container?.start()
        logger.info { "Listening to topic $topic" }
    }

    fun stop() {
        container?.stop()
        container = null
    }

    fun resume() = container?.resume()
    fun pause() = container?.pause()
    fun isPaused() = container?.isContainerPaused
    fun isRunning() = container?.isRunning

    override fun onMessage(data: ConsumerRecord<String, String>) {
        val event = try {
            KafkaEvents.toEvent(data.key())
        } catch (e: IllegalArgumentException) {
            logger.error { "${data.key()} is not a member of KafkaEvents" }
            null
        }
        event?.let {
            val deserialized = deserializer.deserialize(it, data.value())
            val dz = data.toDeserializedConsumerRecord(it, deserialized)
            onMessageReceived(dz)
        }
    }

}

private fun <K, V, KDez, VDez> ConsumerRecord<K, V>.toDeserializedConsumerRecord(
    keyzz: KDez,
    valuezz: VDez
): DeserializedConsumerRecord<KDez, VDez> {
    return DeserializedConsumerRecord(
        topic = this.topic(),
        partition = this.partition(),
        offset = this.offset(),
        timestamp = this.timestamp(),
        timestampType = this.timestampType(),
        headers = this.headers(),
        key = keyzz,
        value = valuezz,
        leaderEpoch = this.leaderEpoch().orElse(null)
    )
}
