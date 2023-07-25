package no.iktdev.streamit.content.reader.collector

import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.library.db.query.SubtitleQuery
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.SimpleMessageListener
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

private val logger = KotlinLogging.logger {}

class ExtractedSubtitleConsumer : DefaultKafkaReader("collectorConsumerExtractedSubtitle") {

    private val listener = object: SimpleMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accepts = listOf(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event)
    ) {
        override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
            val workResult = data.value().dataAs(ExtractWork::class.java)
            if (!data.value().isSuccessful() || workResult == null) {
                return
            }

            val of = File(workResult.outFile)
            val status = transaction {
                SubtitleQuery(
                    title = of.nameWithoutExtension,
                    language = workResult.language,
                    collection = workResult.collection,
                    format = of.extension.uppercase()
                ).insertAndGetStatus()
            }
            val message = Message(referenceId = data.value()?.referenceId ?: "M.I.A", status = Status(statusType = StatusType.SUCCESS))

            if (status) {
                produceMessage(KafkaEvents.EVENT_COLLECTOR_SUBTITLE_STORED, message, null)
                logger.info { "Stored ${File(workResult.outFile).absolutePath} subtitle" }
            } else {
                produceErrorMessage(KafkaEvents.EVENT_COLLECTOR_SUBTITLE_STORED, message.withNewStatus(status = Status(statusType = StatusType.ERROR)), "Unknown, see log")
                logger.error { "Failed to store ${File(workResult.outFile).absolutePath} subtitle" }
            }
        }
    }

    init {
        listener.listen()
    }

    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE)
    }
}