package no.iktdev.streamit.content.reader.collector

import kotlinx.coroutines.runBlocking
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.Downloader
import no.iktdev.streamit.content.common.SequentialKafkaReader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.Metadata

import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.library.db.query.*
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.SimpleMessageListener
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import no.iktdev.streamit.library.kafka.listener.sequential.SequentialMessageListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.io.File

class ExtractedSubtitleConsumer : DefaultKafkaReader("collectorConsumerSubtitle") {

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
            SubtitleQuery(
                title = of.nameWithoutExtension,
                language = workResult.language,
                collection = workResult.collection,
                format = of.extension.uppercase()
            ).insertAndGetStatus()
        }
    }

    init {
        listener.listen()
    }

    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE)
    }
}