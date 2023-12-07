package no.iktdev.mediaprocessing.shared.kafka.core

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.KafkaMessageListenerContainer
import org.springframework.kafka.listener.MessageListener
import java.lang.IllegalArgumentException
import java.util.*

open class DefaultMessageListener(
    open val topic: String,
    open val consumer: DefaultConsumer = DefaultConsumer(subId = UUID.randomUUID().toString()),
    open var onMessageReceived: (DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) -> Unit = {}
)
    : MessageListener<String, String> {


    private val logger = KotlinLogging.logger {}
    private val deserializer = DeserializingRegistry()

    protected var container: KafkaMessageListenerContainer<String, String>? = null

    open fun listen() {
        val listener = consumer.consumerFactoryListener()
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
            KafkaEvents.valueOf(data.key())
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

private fun <K, V, KDez, VDez> ConsumerRecord<K, V>.toDeserializedConsumerRecord(keyzz: KDez, valuezz: VDez): DeserializedConsumerRecord<KDez, VDez> {
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
