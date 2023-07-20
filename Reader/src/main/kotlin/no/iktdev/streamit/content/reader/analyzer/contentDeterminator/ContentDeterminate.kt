package no.iktdev.streamit.content.reader.analyzer.contentDeterminator

import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.content.common.dto.Metadata
import no.iktdev.streamit.content.reader.DefaultKafkaReader
import no.iktdev.streamit.content.reader.fileWatcher.FileWatcher
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.sequential.ISequentialMessageEvent
import no.iktdev.streamit.library.kafka.listener.sequential.SequentialMessageListener
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ContentDeterminate: DefaultKafkaReader("contentDeterminate"), ISequentialMessageEvent {

    final val mainListener = object : SequentialMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accept = KafkaEvents.EVENT_READER_RECEIVED_FILE.event,
        subAccepts = listOf(KafkaEvents.EVENT_METADATA_OBTAINED.event),
        deserializers = Deserializers().getDeserializers(),
        this
    ) {}

    init {
        mainListener.listen()
    }



    override fun getRequiredMessages(): List<String> {
        return mainListener.subAccepts + listOf(mainListener.accept)
    }

    override fun onAllMessagesProcessed(referenceId: String, result: Map<String, Message?>) {
        logger.info { "All messages are received" }

        val initMessage = result[KafkaEvents.EVENT_READER_RECEIVED_FILE.event]
        if (initMessage == null) {
            produceErrorMessage(Message(referenceId = referenceId, status = Status(statusType = StatusType.ERROR)), "Initiator message not found!")
            return
        }
        val fileResult = initMessage.data as FileWatcher.FileResult?
        if (fileResult == null) {
            produceErrorMessage(initMessage, "FileResult is either null or not deserializable!")
            return
        }

        val metadataMessage = result[KafkaEvents.EVENT_METADATA_OBTAINED.event]
        val metadata = if (metadataMessage?.status?.statusType == StatusType.SUCCESS) metadataMessage.data as Metadata? else null

        val baseFileName = if (metadata?.type == null) {
            FileNameDeterminate(fileResult.title, fileResult.sanitizedName).getDeterminedFileName()
        } else if (metadata.type.lowercase() == "movie") {
            FileNameDeterminate(fileResult.title, fileResult.sanitizedName, FileNameDeterminate.ContentType.MOVIE).getDeterminedFileName()
        } else {
            FileNameDeterminate(fileResult.title, fileResult.sanitizedName, FileNameDeterminate.ContentType.SERIE).getDeterminedFileName()
        }

        val out = ContentOutName(baseFileName)
        produceMessage(KafkaEvents.EVENT_READER_DETERMINED_FILENAME, initMessage, out)

    }

}