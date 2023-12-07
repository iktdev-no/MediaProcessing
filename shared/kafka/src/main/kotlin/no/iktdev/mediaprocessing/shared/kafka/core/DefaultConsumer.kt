package no.iktdev.mediaprocessing.shared.kafka.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.contract.ffmpeg.ParsedMediaStreams
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties.AckMode
import kotlin.reflect.full.findAnnotation
import java.util.UUID
import kotlin.reflect.KClass

open class DefaultConsumer(val subId: String = UUID.randomUUID().toString()) {
    val log = KotlinLogging.logger {}

    var autoCommit: Boolean = true
    var ackModeOverride: AckMode? = null

    open fun consumerFactory(): DefaultKafkaConsumerFactory<String, String> {
        val config: MutableMap<String, Any> = HashMap()
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaEnv.servers
        config[ConsumerConfig.GROUP_ID_CONFIG] = "${KafkaEnv.consumerId}:$subId"
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = autoCommit
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = KafkaEnv.loadMessages

        return DefaultKafkaConsumerFactory(config, StringDeserializer(), StringDeserializer())

    }

    fun consumerFactoryListener(): ConcurrentKafkaListenerContainerFactory<String, String> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory()
        ackModeOverride?.let {
            factory.containerProperties.ackMode = it
        }
        return factory
    }
}