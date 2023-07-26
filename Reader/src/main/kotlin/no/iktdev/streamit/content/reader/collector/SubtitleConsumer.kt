package no.iktdev.streamit.content.reader.collector

import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.reader.work.ConvertWork
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
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class SubtitleConsumer : DefaultKafkaReader("collectorConsumerExtractedSubtitle") {

    private val listener = object: SimpleMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accepts = listOf(
            KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event,
            KafkaEvents.EVENT_CONVERTER_ENDED_SUBTITLE_FILE.event
        )
    ) {
        override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
            val referenceId = data.value().referenceId
            if (data.key() == KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event) {
                val work = data.value().dataAs(ExtractWork::class.java)
                if (work == null) {
                    logger.info { "Event: ${data.key()} value is null" }
                } else {
                    storeExtractWork(referenceId, work)
                }
            } else if (data.key() == KafkaEvents.EVENT_CONVERTER_ENDED_SUBTITLE_FILE.event) {
                val work = data.value().dataAs(ConvertWork::class.java)
                if (work == null) {
                    logger.info { "Event: ${data.key()} value is null" }
                } else {
                    storeConvertWork(referenceId, work)
                }
            } else {
                if (data.value().isSuccessful()) {
                    logger.warn { "Event: ${data.key()} is not captured" }
                } else {
                    logger.info { "Event: ${data.key()} is not ${StatusType.SUCCESS.name}" }
                }
            }
        }
    }

    init {
        listener.listen()
    }

    fun produceMessage(referenceId: String, outFile: String, statusType: StatusType, result: Any?) {
        if (statusType == StatusType.SUCCESS) {
            produceSuccessMessage(KafkaEvents.EVENT_COLLECTOR_SUBTITLE_STORED, referenceId)
            logger.info { "Stored ${File(outFile).absolutePath} subtitle" }
        } else {
            produceErrorMessage(KafkaEvents.EVENT_COLLECTOR_SUBTITLE_STORED, Message(referenceId, Status(statusType), result), "See log")
            logger.error { "Failed to store ${File(outFile).absolutePath} subtitle" }
        }
    }

    fun storeExtractWork(referenceId: String, work: ExtractWork) {
        val of = File(work.outFile)
        val status = transaction {
            SubtitleQuery(
                title = of.nameWithoutExtension,
                language = work.language,
                collection = work.collection,
                format = of.extension.uppercase()
            )
                .insertAndGetStatus()
        }
        produceMessage(referenceId, work.outFile, if (status) StatusType.SUCCESS else StatusType.ERROR, "Store Extracted: $status")
    }

    fun storeConvertWork(referenceId: String, work: ConvertWork) {
        val of = File(work.outFile)
        val status = transaction {
            SubtitleQuery(
                title = of.nameWithoutExtension,
                language = work.language,
                collection = work.collection,
                format = of.extension.uppercase()
            )
                .insertAndGetStatus()
        }
        produceMessage(referenceId, work.outFile, if (status) StatusType.SUCCESS else StatusType.ERROR, "Store Converted: $status")
    }


    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(
            KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE,
            KafkaEvents.EVENT_CONVERTER_ENDED_SUBTITLE_FILE
        )
    }
}