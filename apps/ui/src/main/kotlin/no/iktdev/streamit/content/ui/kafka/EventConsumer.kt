package no.iktdev.streamit.content.ui.kafka

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.ui.kafka.converter.EventDataConverter
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.listener.ManualAcknowledgeMessageListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.stereotype.Service

@Service
class EventConsumer: DefaultKafkaReader() {
    @Autowired private lateinit var converter: EventDataConverter
    companion object {
        val idAndEvents: MutableMap<String, MutableMap<String, Message>> = mutableMapOf()
    }

    private val log = KotlinLogging.logger {}

    private final val listener = object : ManualAcknowledgeMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accepts = listOf()
    ) {
        override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
            applyUpdate(data.value().referenceId, data.key(), data.value())
            log.info { data.key() + Gson().toJson(data.value()) }
            converter.convertEventToObject(data.value().referenceId)
        }
    }

    private fun applyUpdate(referenceId: String, eventKey: String, value: Message) {
        val existingData = idAndEvents[referenceId] ?: mutableMapOf()
        existingData[eventKey] = value
        idAndEvents[referenceId] = existingData
    }

    init {
        defaultConsumer.autoCommit = false
        defaultConsumer.ackModeOverride = ContainerProperties.AckMode.MANUAL
        listener.listen()
    }
}