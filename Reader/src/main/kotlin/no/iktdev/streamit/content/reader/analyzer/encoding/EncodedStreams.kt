package no.iktdev.streamit.content.reader.analyzer.encoding

import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.common.streams.MediaStreams
import no.iktdev.streamit.content.reader.analyzer.encoding.helpers.EncodeArgumentSelector
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.collector.CollectorMessageListener
import no.iktdev.streamit.library.kafka.listener.collector.ICollectedMessagesEvent
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import no.iktdev.streamit.library.kafka.listener.sequential.ISequentialMessageEvent
import no.iktdev.streamit.library.kafka.listener.sequential.SequentialMessageListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class EncodedStreams : DefaultKafkaReader("streamSelector"), ICollectedMessagesEvent<ResultCollection> {

    val collectionListener = CollectorMessageListener<ResultCollection>(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        initiatorEvent = KafkaEvents.EVENT_READER_RECEIVED_FILE,
        completionEvent = KafkaEvents.EVENT_READER_DETERMINED_FILENAME,
        listener = this,
        eventCollectionClass = ResultCollection::class.java
    )

    init {
        collectionListener.listen()
    }

    fun createEncodeWork(referenceId: String, collection: String?, inFile: String?, streams: MediaStreams?, outFileName: String?) {
        if (inFile.isNullOrBlank()) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO, referenceId, "No input file received"); return
        }
        if (streams == null) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO, referenceId, "No input streams received"); return
        }
        if (outFileName.isNullOrBlank()) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO, referenceId, "No output file name received!"); return
        }
        if (collection.isNullOrBlank()) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO, referenceId, "No collection provided for file!"); return
        }

        val encodeInformation =
            EncodeArgumentSelector(collection = collection, inputFile = inFile, streams = streams, outFileName = outFileName)

        val videoInstructions = encodeInformation.getVideoAndAudioArguments()
        if (videoInstructions == null) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO, referenceId, "Failed to generate Video Arguments Bundle")
            return
        }
        produceMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_VIDEO, Message(referenceId, Status(StatusType.SUCCESS)), videoInstructions)

    }

    fun createExtractWork(referenceId: String, collection: String?, inFile: String?, streams: MediaStreams?, outFileName: String?) {
        if (inFile.isNullOrBlank()) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE, referenceId, "No input file received"); return
        }
        if (streams == null) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE, referenceId, "No input streams received"); return
        }
        if (outFileName.isNullOrBlank()) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE, referenceId, "No output file name received!"); return
        }
        if (collection.isNullOrBlank()) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE, referenceId, "No collection provided for file!"); return
        }

        val argsSelector =  EncodeArgumentSelector(collection = collection, inputFile = inFile, streams = streams, outFileName = outFileName)
        val items = argsSelector.getSubtitleArguments()
        if (argsSelector == null || items.isEmpty()) {
            produceErrorMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE, referenceId, "Failed to generate Subtitle Arguments Bundle")
            return
        }

        argsSelector.getSubtitleArguments().forEach {
            produceMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED_SUBTITLE, Message(referenceId, Status(StatusType.SUCCESS)), it)

        }

    }


    final override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(
            KafkaEvents.EVENT_READER_RECEIVED_FILE,
            KafkaEvents.EVENT_READER_RECEIVED_STREAMS,
            KafkaEvents.EVENT_READER_DETERMINED_FILENAME
        )
    }

    override fun onCollectionCompleted(collection: ResultCollection?) {
        logger.info { "Collection received" }
        val collectedEvents = collection?.getRecords()?.map { it.key() }?.toList() ?: emptyList()
        logger.info { "\nConsumer $subId collected:\n${collectedEvents.joinToString { "\n\t" }}" }

        val referenceId = collection?.getRecords()?.firstOrNull()?.value()?.referenceId
        if (referenceId == null) {
            logger.warn { "referenceId is null, throwing collection" }
            return
        }
        val outFileNameWithoutExtension: String? = if (collection.getFileName() != null) {
            collection.getFileName()?.baseName
        } else {
            logger.info { "Getting filename from ${KafkaEvents.EVENT_READER_DETERMINED_FILENAME.event} resulted in null. Falling back to sanitized name" }
            collection.getFileResult()?.sanitizedName
        }

        createEncodeWork(referenceId, collection.getFileResult()?.title, collection.getFileResult()?.file, collection.getStreams(), outFileNameWithoutExtension)
        createExtractWork(referenceId, collection.getFileResult()?.title, collection.getFileResult()?.file, collection.getStreams(), outFileNameWithoutExtension)
    }

}