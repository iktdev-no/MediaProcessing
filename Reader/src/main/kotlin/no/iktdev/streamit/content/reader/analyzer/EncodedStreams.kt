package no.iktdev.streamit.content.reader.analyzer

import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.reader.analyzer.encoding.EncodeArgumentSelector
import no.iktdev.streamit.content.reader.analyzer.encoding.dto.EncodeInformation
import no.iktdev.streamit.library.kafka.KnownEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.listener.pooled.IPooledEvents
import no.iktdev.streamit.library.kafka.listener.pooled.PooledEventMessageListener
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import java.io.File

@Service
class EncodedStreams: IPooledEvents.OnEventsReceived {

    val messageProducer = DefaultProducer(CommonConfig.kafkaTopic)

    val defaultConsumer = DefaultConsumer(subId = "encodedStreams").apply {
        autoCommit = false
    }

    init {
        val ackListener = PooledEventMessageListener(
            topic = CommonConfig.kafkaTopic, consumer = defaultConsumer,
            mainFilter = KnownEvents.EVENT_READER_RECEIVED_FILE.event,
            subFilter = listOf(KnownEvents.EVENT_READER_RECEIVED_STREAMS.event),
            event = this
        )
        ackListener.listen()
    }

    override fun areAllMessagesReceived(recordedEvents: MutableMap<String, StatusType>): Boolean {
        val expected = listOf(KnownEvents.EVENT_READER_RECEIVED_FILE.event, KnownEvents.EVENT_READER_RECEIVED_STREAMS.event)
        return expected.containsAll(recordedEvents.keys)
    }

    private fun produceErrorMessage(referenceId: String, reason: String) {
        val message = Message(referenceId = referenceId,
            Status(statusType = StatusType.ERROR, message = reason)
        )
        messageProducer.sendMessage(KnownEvents.EVENT_READER_ENCODE_GENERATED.event, message)
    }

    private fun produceEncodeMessage(referenceId: String, data: EncodeInformation?) {
        val message = Message(referenceId = referenceId,
            Status(statusType = if (data != null) StatusType.SUCCESS else StatusType.IGNORED),
            data = data
        )
        messageProducer.sendMessage(KnownEvents.EVENT_READER_ENCODE_GENERATED.event, message)
    }

    override fun onAllEventsConsumed(referenceId: String, records: MutableList<ConsumerRecord<String, Message>>) {
        val parser = EncodeStreamsMessageParser()
        val fileResult = parser.getFileNameFromEvent(records)
        if (fileResult == null) {
            produceErrorMessage(referenceId, "FileResult is either null or not deserializable!")
            return
        }
        val outFileName = fileResult.desiredNewName.ifBlank { File(fileResult.file).nameWithoutExtension }
        val streams = parser.getMediaStreamsFromEvent(records)
        if (streams == null) {
            produceErrorMessage(referenceId, "No streams received!")
            return
        }

        val encodeInformation = EncodeArgumentSelector(inputFile = fileResult.file, streams = streams, outFileName = outFileName)
        produceEncodeMessage(referenceId, encodeInformation.getVideoAndAudioArguments())
        encodeInformation.getSubtitleArguments().forEach { s ->
            produceEncodeMessage(referenceId, s)
        }




    }






}