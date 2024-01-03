package no.iktdev.mediaprocessing.shared.kafka.core

import mu.KotlinLogging
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.*

@Configuration
open class KafkaImplementation {
    private val log = KotlinLogging.logger {}

    @Bean
    open fun admin() = KafkaAdmin(mapOf(
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to KafkaEnv.servers
    ))

    @Bean
    open fun producerFactory(): ProducerFactory<String, String> {
        val config: MutableMap<String, Any> = HashMap()
        config[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaEnv.servers
        config[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        config[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        //log.info { config }
        return DefaultKafkaProducerFactory(config)
    }
    @Bean
    open fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    open fun consumerFactory(): ConsumerFactory<String, String> {
        val config: MutableMap<String, Any> = HashMap()
        config[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = KafkaEnv.servers
        config[ConsumerConfig.GROUP_ID_CONFIG] = KafkaEnv.consumerId
        config[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        config[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = KafkaEnv.loadMessages
        config[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = KafkaEnv.sessionTimeOutMilliseconds
        config[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = KafkaEnv.heartbeatIntervalMilliseconds
        //log.info { config }
        return DefaultKafkaConsumerFactory(config, StringDeserializer(), StringDeserializer())
    }
}