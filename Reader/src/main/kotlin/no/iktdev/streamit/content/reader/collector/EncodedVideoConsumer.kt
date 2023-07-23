package no.iktdev.streamit.content.reader.collector

import kotlinx.coroutines.runBlocking
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.Downloader
import no.iktdev.streamit.content.common.SequentialKafkaReader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.Metadata
import no.iktdev.streamit.content.common.dto.reader.EpisodeInfo
import no.iktdev.streamit.content.common.dto.reader.FileResult
import no.iktdev.streamit.content.common.dto.reader.work.EncodeWork
import no.iktdev.streamit.library.db.query.*
import no.iktdev.streamit.library.db.tables.catalog
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import no.iktdev.streamit.library.kafka.listener.sequential.SequentialMessageListener
import org.springframework.stereotype.Service
import java.io.File

@Service
class EncodedVideoConsumer: SequentialKafkaReader("collectorConsumerVideo") {
    override val accept: KafkaEvents
        get() = KafkaEvents.EVENT_READER_RECEIVED_FILE
    override val subAccepts: List<KafkaEvents>
        get() = listOf(
            KafkaEvents.EVENT_METADATA_OBTAINED,
            KafkaEvents.EVENT_READER_DETERMINED_SERIE,
            KafkaEvents.EVENT_READER_DETERMINED_MOVIE,
            KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE
        )


    val listener = object: SequentialMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accept = accept.event,
        subAccepts = subAccepts.map { it.event },
        deserializers = loadDeserializers(),
        validity = 86400000,
        listener =this
    ) {}

    init {
        listener.listen()
    }


    override fun getRequiredMessages(): List<String> {
        return listOf(accept.event) + subAccepts.map { it.event }
    }

    override fun onAllMessagesProcessed(referenceId: String, result: Map<String, Message?>) {
        val metadata = result[KafkaEvents.EVENT_METADATA_OBTAINED.event]?.data as Metadata?
        val fileData = result[KafkaEvents.EVENT_READER_RECEIVED_FILE.event]?.data as FileResult?
        val encodeStatus = result[KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE.event]?.status?.statusType ?: StatusType.ERROR
        val encodeWork = result[KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE.event]?.data as EncodeWork?


        if (fileData == null || encodeStatus != StatusType.SUCCESS || encodeWork == null) {
            return
        }

        val videoFileNameWithExtension = File(encodeWork.outFile).name

        val contentType = metadata?.type ?: return
        val iid = if (contentType == "movie") MovieQuery(videoFileNameWithExtension).insertAndGetId() else null

        val serieData = result[KafkaEvents.EVENT_READER_DETERMINED_SERIE.event]?.data as EpisodeInfo?
        if (serieData != null) {
            val success = SerieQuery(serieData.title, serieData.episode, serieData.season, fileData.title,  videoFileNameWithExtension).insertAndGetStatus()
            if (!success)
                return
        }

        val coverFile = metadata?.cover?.let { coverUrl ->
            runBlocking {
                try {
                    Downloader(coverUrl, CommonConfig.outgoingContent, fileData.title).download()
                } catch (e: Exception) {
                    // No cover
                    null
                }
            }
        }
        val metaGenre = metadata.genres
        val gq = GenreQuery(*metaGenre.toTypedArray())
        gq.insertAndGetIds()
        val gids = gq.getIds().joinToString(",")

        val cq = CatalogQuery(
            title = fileData.title,
            cover = coverFile?.name,
            type = contentType,
            collection = fileData.title,
            iid = iid,
            genres = gids
        )
        val cid = cq.insertAndGetId() ?: cq.getId() ?: return
        if (!metadata.summary.isNullOrBlank()) {
            val summary = metadata.summary ?: return
            SummaryQuery(
                cid = cid,
                language = "eng", // TODO: Fix later,
                description = summary
            )
        }
    }



    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(*subAccepts.toTypedArray())
    }
}