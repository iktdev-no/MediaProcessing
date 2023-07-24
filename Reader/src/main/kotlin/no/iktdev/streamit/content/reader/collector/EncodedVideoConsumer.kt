package no.iktdev.streamit.content.reader.collector

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.Downloader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.library.db.query.*
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.collector.CollectorMessageListener
import no.iktdev.streamit.library.kafka.listener.collector.ICollectedMessagesEvent
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class EncodedVideoConsumer: DefaultKafkaReader("collectorConsumerEncodedVideo"), ICollectedMessagesEvent<ResultCollection> {

    val listener = CollectorMessageListener<ResultCollection>(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        initiatorEvent = KafkaEvents.EVENT_READER_RECEIVED_FILE,
        completionEvent = KafkaEvents.EVENT_ENCODER_ENDED_VIDEO_FILE,
        acceptsFilter = listOf(
            KafkaEvents.EVENT_METADATA_OBTAINED,
            KafkaEvents.EVENT_READER_DETERMINED_SERIE,
            KafkaEvents.EVENT_READER_DETERMINED_MOVIE,
        ),
        listener = this,
        eventCollectionClass = ResultCollection::class.java
    )


    init {
        listener.listen()
    }





    override fun loadDeserializers(): Map<String, IMessageDataDeserialization<*>> {
        return DeserializerRegistry.getEventToDeserializer(*listener.acceptsFilter.toTypedArray(), listener.initiatorEvent, listener.completionEvent)
    }

    override fun onCollectionCompleted(collection: ResultCollection?) {
        val metadata = collection?.getMetadata()
        val fileData = collection?.getFileResult()
        val encodeWork = collection?.getEncodeWork()
        val serieData = collection?.getSerieInfo()
        val movieData = collection?.getMovieInfo()


        if (fileData == null || encodeWork == null || collection.getReferenceId() == null) {
            logger.error { "Required data is null, as it has either status as non successful or simply missing" }
            return
        }

        val videoFileNameWithExtension = File(encodeWork.outFile).name


        val contentType = metadata?.type ?: return
        val iid = if (contentType == "movie") transaction {
            MovieQuery(videoFileNameWithExtension).insertAndGetId()
        } else null

        if (serieData != null) {
            val success = transaction {
                SerieQuery(serieData.title, serieData.episode, serieData.season, fileData.title,  videoFileNameWithExtension)
                    .insertAndGetStatus()
            }
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
        transaction {
            gq.insertAndGetIds()
        }
        val gids = transaction { gq.getIds().joinToString(",") }

        val cq = CatalogQuery(
            title = fileData.title,
            cover = coverFile?.name,
            type = contentType,
            collection = fileData.title,
            iid = iid,
            genres = gids
        )
        val cid = transaction { cq.insertAndGetId() ?: cq.getId() } ?: return
        if (!metadata.summary.isNullOrBlank()) {
            val summary = metadata.summary ?: return
            transaction {
                SummaryQuery(
                    cid = cid,
                    language = "eng", // TODO: Fix later,
                    description = summary
                )
            }
        }

        val message = Message(referenceId = collection.getReferenceId() ?: "M.I.A", status = Status(StatusType.SUCCESS))
        produceMessage(KafkaEvents.EVENT_COLLECTOR_VIDEO_STORED, message, null)
        logger.info { "Stored ${metadata.title} video" }
    }
}