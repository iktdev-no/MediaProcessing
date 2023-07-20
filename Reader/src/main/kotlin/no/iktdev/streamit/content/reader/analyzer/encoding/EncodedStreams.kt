package no.iktdev.streamit.content.reader.analyzer.encoding

import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.content.common.streams.MediaStreams
import no.iktdev.streamit.content.reader.analyzer.encoding.dto.EncodeInformation
import no.iktdev.streamit.content.reader.analyzer.encoding.helpers.EncodeArgumentSelector
import no.iktdev.streamit.content.reader.fileWatcher.FileWatcher
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.sequential.ISequentialMessageEvent
import no.iktdev.streamit.library.kafka.listener.sequential.SequentialMessageListener
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class EncodedStreams : ISequentialMessageEvent {

    val messageProducer = DefaultProducer(CommonConfig.kafkaTopic)

    final val defaultConsumer = DefaultConsumer(subId = "encodedStreams")


    final val mainListener = object : SequentialMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accept = KafkaEvents.EVENT_READER_RECEIVED_FILE.event,
        subAccepts = listOf(
            KafkaEvents.EVENT_READER_RECEIVED_STREAMS.event,
            KafkaEvents.EVENT_READER_DETERMINED_FILENAME.event
        ),
        deserializers = EncodedDeserializers().getDeserializers(),
        this
    ) {}

    init {
        mainListener.listen()
    }


    override fun getRequiredMessages(): List<String> {
        return listOf(KafkaEvents.EVENT_READER_RECEIVED_FILE.event, KafkaEvents.EVENT_READER_RECEIVED_STREAMS.event)
    }

    override fun onAllMessagesProcessed(referenceId: String, result: Map<String, Message?>) {
        logger.info { "All messages are received" }
        val baseMessage = result[KafkaEvents.EVENT_READER_RECEIVED_FILE.event]
        if (baseMessage == null) {
            produceErrorMessage(
                Message(referenceId = referenceId, status = Status(statusType = StatusType.ERROR)),
                "Initiator message not found!"
            )
            return
        }

        if (result.values.any { it?.status?.statusType != StatusType.SUCCESS }) {
            produceErrorMessage(
                Message(referenceId = referenceId, status = Status(statusType = StatusType.ERROR)),
                "Failed messages found!"
            )
            return
        }
        val fileResult = baseMessage.data as FileWatcher.FileResult?
        if (fileResult == null) {
            produceErrorMessage(baseMessage, "FileResult is either null or not deserializable!")
            return
        }

        val determinedfnm = result[KafkaEvents.EVENT_READER_DETERMINED_FILENAME.event]
        val determinedFileName = determinedfnm?.data as ContentOutName

        val outFileName = if (determinedfnm.status.statusType == StatusType.SUCCESS)
            determinedFileName.baseName
        else fileResult.sanitizedName.ifBlank { File(fileResult.file).nameWithoutExtension }


        val streams = result[KafkaEvents.EVENT_READER_RECEIVED_STREAMS.event]?.data as MediaStreams?
        if (streams == null) {
            produceErrorMessage(baseMessage, "No streams received!")
            return
        }

        val encodeInformation =
            EncodeArgumentSelector(inputFile = fileResult.file, streams = streams, outFileName = outFileName)
        produceEncodeMessage(baseMessage, encodeInformation.getVideoAndAudioArguments())
        encodeInformation.getSubtitleArguments().forEach { s ->
            produceEncodeMessage(baseMessage, s)
        }
    }


    private fun produceErrorMessage(baseMessage: Message, reason: String) {
        val message = Message(
            referenceId = baseMessage.referenceId,
            actionType = baseMessage.actionType,
            Status(statusType = StatusType.ERROR, message = reason)
        )
        messageProducer.sendMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED.event, message)
    }

    private fun produceEncodeMessage(baseMessage: Message, data: EncodeInformation?) {
        val message = Message(
            referenceId = baseMessage.referenceId,
            actionType = baseMessage.actionType,
            Status(statusType = if (data != null) StatusType.SUCCESS else StatusType.IGNORED),
            data = data
        )
        messageProducer.sendMessage(KafkaEvents.EVENT_READER_ENCODE_GENERATED.event, message)
    }


}