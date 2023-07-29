package no.iktdev.streamit.content.reader.analyzer.contentDeterminator

import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.deserializers.FileResultDeserializer
import no.iktdev.streamit.content.common.deserializers.MetadataResultDeserializer
import no.iktdev.streamit.content.common.dto.ContentOutName
import no.iktdev.streamit.content.common.dto.Metadata
import no.iktdev.streamit.content.common.dto.reader.EpisodeInfo
import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.common.dto.reader.MovieInfo
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
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
        deserializers = loadDeserializers(),
        listener = this
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
        if (initMessage == null || initMessage.status.statusType != StatusType.SUCCESS) {
            produceErrorMessage(
                KafkaEvents.EVENT_READER_DETERMINED_FILENAME,
                Message(referenceId = referenceId, status = Status(statusType = StatusType.ERROR)),
                "Initiator message not found!"
            )
            return
        }
        val fileResult = initMessage.data as FileResult?
        if (fileResult == null) {
            produceErrorMessage(
                KafkaEvents.EVENT_READER_DETERMINED_FILENAME,
                initMessage,
                "FileResult is either null or not deserializable!"
            )
            return
        }

        val metadataMessage = result[KafkaEvents.EVENT_METADATA_OBTAINED.event]
        val metadata =
            if (metadataMessage?.status?.statusType == StatusType.SUCCESS) metadataMessage.data as Metadata? else null


        // Due to the fact that the sources might say serie, but it is not a serie input we will give serie a try then default to movie


        val videoInfo = when (metadata?.type) {
            "serie" -> {
                FileNameDeterminate(
                    fileResult.title,
                    fileResult.sanitizedName,
                    FileNameDeterminate.ContentType.SERIE
                ).getDeterminedVideoInfo()
            }

            "movie" -> {
                FileNameDeterminate(
                    fileResult.title,
                    fileResult.sanitizedName,
                    FileNameDeterminate.ContentType.MOVIE
                ).getDeterminedVideoInfo()
            }

            else -> null
        } ?: FileNameDeterminate(fileResult.title, fileResult.sanitizedName).getDeterminedVideoInfo()

        if (videoInfo == null) {
            produceErrorMessage(KafkaEvents.EVENT_READER_DETERMINED_FILENAME, initMessage, "VideoInfo is null.")
            return
        }



        if (videoInfo is EpisodeInfo) {
            produceSuccessMessage(KafkaEvents.EVENT_READER_DETERMINED_SERIE, referenceId, videoInfo)
        } else if (videoInfo is MovieInfo) {
            produceSuccessMessage(KafkaEvents.EVENT_READER_DETERMINED_MOVIE, referenceId, videoInfo)
        }

        val out = ContentOutName(videoInfo.fullName)
        produceSuccessMessage(KafkaEvents.EVENT_READER_DETERMINED_FILENAME, referenceId, out)
    }

    final override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return mutableMapOf(
            KafkaEvents.EVENT_READER_RECEIVED_FILE.event to FileResultDeserializer(),
            KafkaEvents.EVENT_METADATA_OBTAINED.event to MetadataResultDeserializer()
        )
    }

}