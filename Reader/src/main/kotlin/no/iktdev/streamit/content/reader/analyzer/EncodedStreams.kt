package no.iktdev.streamit.content.reader.analyzer

import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.streams.MediaStreams
import no.iktdev.streamit.content.reader.analyzer.encoding.EncodeArgumentSelector
import no.iktdev.streamit.content.reader.analyzer.encoding.dto.EncodeInformation
import no.iktdev.streamit.content.reader.fileWatcher.FileWatcher
import no.iktdev.streamit.library.kafka.KnownEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.dto.ActionType
import no.iktdev.streamit.library.kafka.listener.sequential.ISequentialMessageEvent
import no.iktdev.streamit.library.kafka.listener.sequential.SequentialMessageListener
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class EncodedStreams : ISequentialMessageEvent {

    val messageProducer = DefaultProducer(CommonConfig.kafkaTopic)

    final val defaultConsumer = DefaultConsumer(subId = "encodedStreams").apply {
        autoCommit = false
    }


    final val mainListener = object : SequentialMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accept = KnownEvents.EVENT_READER_RECEIVED_FILE.event,
        subAccepts = listOf(KnownEvents.EVENT_READER_RECEIVED_STREAMS.event),
        deserializers = EncodedDeserializers().getDeserializers(),
        this
    ) {}

    init {
        mainListener.listen()
    }


    private fun produceErrorMessage(baseMessage: Message, reason: String) {
        val message = Message(
            referenceId = baseMessage.referenceId,
            actionType = baseMessage.actionType,
            Status(statusType = StatusType.ERROR, message = reason)
        )
        messageProducer.sendMessage(KnownEvents.EVENT_READER_ENCODE_GENERATED.event, message)
    }

    private fun produceEncodeMessage(baseMessage: Message, data: EncodeInformation?) {
        val message = Message(
            referenceId = baseMessage.referenceId,
            actionType = baseMessage.actionType,
            Status(statusType = if (data != null) StatusType.SUCCESS else StatusType.IGNORED),
            data = data
        )
        messageProducer.sendMessage(KnownEvents.EVENT_READER_ENCODE_GENERATED.event, message)
    }

    override fun areAllMessagesPresent(currentEvents: List<String>): Boolean {
        val expected = listOf(KnownEvents.EVENT_READER_RECEIVED_FILE.event, KnownEvents.EVENT_READER_RECEIVED_STREAMS.event)
        val waitingFor = expected.filter { !currentEvents.contains(it) }
        return if (waitingFor.isEmpty()) {
            true
        } else {
            logger.info { "Waiting for events: \n ${waitingFor.joinToString("\n\t")}" }
            false
        }
    }

    override fun onAllMessagesProcessed(referenceId: String, result: Map<String, Message?>) {
        logger.info { "All messages are received" }
        val baseMessage = result[KnownEvents.EVENT_READER_RECEIVED_FILE.event]
        if (baseMessage == null) {
            produceErrorMessage(Message(referenceId = referenceId, status = Status(statusType = StatusType.ERROR)), "Initiator message not found!")
            return
        }

        if (result.values.all { it?.status?.statusType == StatusType.SUCCESS }) {

            return
        }
        val fileResult = baseMessage.data as FileWatcher.FileResult?
        if (fileResult == null) {
            produceErrorMessage(baseMessage, "FileResult is either null or not deserializable!")
            return
        }

        val outFileName = fileResult.desiredNewName.ifBlank { File(fileResult.file).nameWithoutExtension }

        val streams = result[KnownEvents.EVENT_READER_RECEIVED_STREAMS.event]?.data as MediaStreams?
        if (streams == null) {
            produceErrorMessage(baseMessage, "No streams received!")
            return
        }

        val encodeInformation = EncodeArgumentSelector(inputFile = fileResult.file, streams = streams, outFileName = outFileName)
        produceEncodeMessage(baseMessage, encodeInformation.getVideoAndAudioArguments())
        encodeInformation.getSubtitleArguments().forEach { s ->
            produceEncodeMessage(baseMessage, s)
        }
    }


}