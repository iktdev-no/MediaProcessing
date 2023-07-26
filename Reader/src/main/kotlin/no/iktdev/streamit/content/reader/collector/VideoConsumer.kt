package no.iktdev.streamit.content.reader.collector

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.Downloader
import no.iktdev.streamit.content.common.deserializers.DeserializerRegistry
import no.iktdev.streamit.content.common.dto.Metadata
import no.iktdev.streamit.content.common.dto.reader.EpisodeInfo
import no.iktdev.streamit.library.db.query.*
import no.iktdev.streamit.library.db.tables.catalog
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.listener.collector.CollectorMessageListener
import no.iktdev.streamit.library.kafka.listener.collector.ICollectedMessagesEvent
import no.iktdev.streamit.library.kafka.listener.deserializer.IMessageDataDeserialization
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class VideoConsumer: DefaultKafkaReader("collectorConsumerEncodedVideo"), ICollectedMessagesEvent<ResultCollection> {

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
        logger.info { "Obtained collection: \n\t${collection?.getRecords()?.map { it.key() }?.joinToString("\n\t")}" }

        if (fileData == null || encodeWork == null || collection.getReferenceId() == null) {
            logger.error { "Required data is null, as it has either status as non successful or simply missing" }
            return
        }
        val videoFileNameWithExtension = File(encodeWork.outFile).name

        val iid = transaction {
            val serieStatus = if (serieData != null) {
                getSerieQueryInstance(serieData, videoFileNameWithExtension)?.insertAndGetStatus() ?: false
            } else true
            if (serieData == null || metadata?.type == "movie") {
                MovieQuery(videoFileNameWithExtension).insertAndGetId()
            } else null
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

        // Serie må alltid fullføres før catalog. dette i tilfelle catalog allerede eksisterer og den thrower slik at transaskjonen blir versertert!

        val status = try {
            transaction {
                val genres = metadata?.let { insertAndGetGenres(it)  }

                val cq = CatalogQuery(
                    title = fileData.title,
                    cover = coverFile?.name,
                    type = if (serieData == null) "movie" else "serie",
                    collection = fileData.title,
                    iid = iid,
                    genres = genres
                )
                catalog.insert {
                    it[title] = fileData.title
                    it[cover] =  coverFile?.name
                    it[type] = if (serieData == null) "movie" else "serie"
                    it[catalog.collection] = fileData.title
                    it[catalog.iid] = iid
                    it[catalog.genres] = genres
                }


                val cqId = cq.getId() ?: throw RuntimeException("No Catalog id found!")
                metadata?.let {
                    val summary = it.summary
                    if (summary != null) {
                        SummaryQuery(cid = cqId, language = "eng", description = summary)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        produceSuccessMessage(KafkaEvents.EVENT_COLLECTOR_VIDEO_STORED, collection.getReferenceId() ?: "M.I.A", status)
        logger.info { "Stored ${encodeWork.outFile} video" }
    }

    /**
     * Needs to be wrapped in transaction
     */
    fun insertAndGetGenres(meta: Metadata): String? {
        val gq = GenreQuery(*meta.genres.toTypedArray())
        gq.insertAndGetIds()
        return gq.getIds().joinToString(",")
    }

    fun getSerieQueryInstance(data: EpisodeInfo?, baseName: String?): SerieQuery? {
        if (data == null || baseName == null) return null
        return SerieQuery(data.title, data.episode, data.season, data.title,  baseName)
    }

}