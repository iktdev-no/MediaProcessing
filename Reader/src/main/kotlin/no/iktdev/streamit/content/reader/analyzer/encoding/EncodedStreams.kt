package no.iktdev.streamit.content.reader.analyzer.encoding

import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.deserializers.ContentOutNameDeserializer
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.deserializers.FileResultDeserializer
import no.iktdev.streamit.content.common.deserializers.MediaStreamsDeserializer
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
import no.iktdev.streamit.library.kafka.listener.deserializer.deserializeIfSuccessful
import no.iktdev.streamit.library.kafka.listener.sequential.ISequentialMessageEvent
import no.iktdev.streamit.library.kafka.listener.sequential.SequentialMessageListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class EncodedStreams : DefaultKafkaReader("streamSelector"), ISequentialMessageEvent {

    val listener = object : SequentialMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accept = KafkaEvents.EVENT_READER_RECEIVED_FILE.event,
        subAccepts = listOf(
            KafkaEvents.EVENT_READER_DETERMINED_FILENAME.event,
            KafkaEvents.EVENT_READER_RECEIVED_STREAMS.event,
            ),
        listener = this,
        deserializers = this.loadDeserializers()
    ) {}

    init {
        listener.listen()
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

    override fun getRequiredMessages(): List<String> {
        return listener.subAccepts + listOf(listener.accept)
    }

    override fun onAllMessagesProcessed(referenceId: String, result: Map<String, Message?>) {
        logger.info { "Collection received" }
        if (result.keys.isEmpty()) {
            logger.error { "\nConsumer $subId collected: is null or empty!" }
        } else {
            logger.info { "\nConsumer $subId collected:\n ${result.keys.joinToString("\n\t")}" }
        }

        val outFileNameWithoutExtension: String? = if (getFileName(result) != null) {
            getFileName(result)?.baseName
        } else {
            logger.info { "Getting filename from ${KafkaEvents.EVENT_READER_DETERMINED_FILENAME.event} resulted in null. Falling back to sanitized name" }
            getFileResult(result)?.sanitizedName
        }

        val fileResult = getFileResult(result)
        createEncodeWork(referenceId, fileResult?.title, fileResult?.file, getStreams(result), outFileNameWithoutExtension)
        createExtractWork(referenceId, fileResult?.title, fileResult?.file, getStreams(result), outFileNameWithoutExtension)
    }

    fun getFileResult(result: Map<String, Message?>): FileResult? {
        val record = result[KafkaEvents.EVENT_READER_RECEIVED_FILE.event] ?: return null
        return FileResultDeserializer().deserializeIfSuccessful(record)
    }

    fun getFileName(result: Map<String, Message?>): ContentOutName? {
        val record = result[KafkaEvents.EVENT_READER_DETERMINED_FILENAME.event] ?: return null
        return ContentOutNameDeserializer().deserializeIfSuccessful(record)
    }

    fun getStreams(result: Map<String, Message?>): MediaStreams? {
        val record = result[KafkaEvents.EVENT_READER_RECEIVED_STREAMS.event] ?: return null
        return MediaStreamsDeserializer().deserializeIfSuccessful(record)
    }
}