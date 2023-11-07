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
            KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_ENDED.event,
            KafkaEvents.EVENT_CONVERTER_SUBTITLE_FILE_ENDED.event
        )
    ) {
        override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
            val referenceId = data.value().referenceId
            if (data.key() == KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_ENDED.event) {
                val work = data.value().dataAs(ExtractWork::class.java)
                if (work == null) {
                    logger.info { "Event: ${data.key()} value is null" }
                } else {
                    storeExtractWork(referenceId, work)
                }
            } else if (data.key() == KafkaEvents.EVENT_CONVERTER_SUBTITLE_FILE_ENDED.event) {
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
            produceSuccessMessage(KafkaEvents.EVENT_COLLECTOR_STORED_SUBTITLE, referenceId)
            logger.info { "Stored ${File(outFile).absolutePath} subtitle" }
        } else {
            produceErrorMessage(KafkaEvents.EVENT_COLLECTOR_STORED_SUBTITLE, Message(referenceId, Status(statusType), result), "See log")
            logger.error { "Failed to store ${File(outFile).absolutePath} subtitle" }
        }
    }

    fun storeExtractWork(referenceId: String, work: ExtractWork) {
        val of = File(work.outFile)
        val status = transaction {
            SubtitleQuery(
                associatedWithVideo = of.nameWithoutExtension,
                language = work.language,
                collection = work.collection,
                format = of.extension.uppercase(),
                file = File(work.outFile).name
            )
                .insertAndGetStatus()
        }
        produceMessage(referenceId, work.outFile, if (status) StatusType.SUCCESS else StatusType.ERROR, "Store Extracted: $status")
    }

    fun storeConvertWork(referenceId: String, work: ConvertWork) {

        val status = transaction {
            work.outFiles.map {
                val of = File(it)
                transaction {
                    SubtitleQuery(
                        associatedWithVideo = of.nameWithoutExtension,
                        language = work.language,
                        collection = work.collection,
                        format = of.extension.uppercase(),
                        file = of.name
                    )
                        .insertAndGetStatus()
                } to it
            }
        }
        val failed = status.filter { !it.first }.map { it.second }
        val success = status.filter { it.first }.map { it.second }

        produceSuccessMessage(KafkaEvents.EVENT_COLLECTOR_STORED_SUBTITLE, referenceId, success)
        produceErrorMessage(KafkaEvents.EVENT_COLLECTOR_STORED_SUBTITLE, Message(referenceId, Status(StatusType.ERROR), failed), "See log")
    }


    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(
            KafkaEvents.EVENT_ENCODER_SUBTITLE_FILE_ENDED,
            KafkaEvents.EVENT_CONVERTER_SUBTITLE_FILE_ENDED
        )
    }
}