package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.mediaprocessing.coordinator.mapping.ProcessMapping
import no.iktdev.mediaprocessing.shared.common.datasource.executeOrException
import no.iktdev.mediaprocessing.shared.common.datasource.executeWithStatus
import no.iktdev.mediaprocessing.shared.common.datasource.withTransaction
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.reader.MetadataDto
import no.iktdev.mediaprocessing.shared.contract.reader.VideoDetails
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents.*
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.streamit.library.db.query.*
import no.iktdev.streamit.library.db.tables.titles
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.insertIgnore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

@Service
class CollectAndStoreTask(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}


    override val producesEvent: KafkaEvents = KafkaEvents.EventCollectAndStore

    override val requiredEvents: List<KafkaEvents> = listOf(
        EventMediaProcessStarted,
        EventMediaProcessCompleted
    )
    override val listensForEvents: List<KafkaEvents> = KafkaEvents.entries



    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        val started = events.lastOrSuccessOf(EventMediaProcessStarted) ?: return null
        val completed = events.lastOrSuccessOf(EventMediaProcessCompleted) ?: return null
        if (!started.data.isSuccess() || !completed.data.isSuccess()) {
            return null
        }
        val mapped = ProcessMapping(events).map() ?: return null
        val collection = mapped.collection ?: return null

        val subtitlesStored = mapped.outputFiles?.subtitles?.let {
            storeSubtitles(collection = collection, subtitles = it)
        } ?: false

        val videoFile = mapped.outputFiles?.video?.let { File(it).name }
        val videoInfo = mapped.videoDetails


        val genres = mapped.metadata?.genres?.let {
            storeAndGetGenres(it)
        }

        val catalogId = mapped.metadata?.let { meta ->
            if (videoInfo == null || videoFile == null)
                null
            else
                storeCatalog(metadata = meta,genres = genres, videoFile = videoFile, videoDetails = videoInfo)
        } ?: return SimpleMessageData(Status.ERROR, "Unable to store catalog when metadata is null", event.eventId)

        mapped.metadata?.let {
            storeMetadata(catalogId = catalogId, metadata = it)
            storeTitles(collection = it.collection, it.title, contentTitles = it.titles)
        }

        return SimpleMessageData(Status.COMPLETED, derivedFromEventId = event.eventId)
    }

    private fun storeSubtitles(collection: String, subtitles: List<String>): Boolean {
        val result = subtitles.map { subtitle ->
            val subtitleFile = File(subtitle)
            val language = subtitleFile.parentFile.name
            subtitle to executeWithStatus(getStoreDatabase()) {
                SubtitleQuery(
                    collection = collection,
                    associatedWithVideo = subtitleFile.nameWithoutExtension,
                    language = language,
                    format = subtitleFile.extension.uppercase(),
                    file = subtitleFile.name
                ).insert()
            }
        }
        return result.none { !it.second }
    }

    private fun storeMetadata(catalogId: Int, metadata: MetadataDto) {
        metadata.summary.forEach {
            withTransaction(getStoreDatabase()) {
                SummaryQuery(
                    cid = catalogId,
                    language = it.language,
                    description = it.summary
                ).insert()
            }
        }
    }

    private fun storeTitles(collection: String, usedTitle: String, contentTitles: List<String>) {
        withTransaction(getStoreDatabase()) {
            titles.insertIgnore {
                it[titles.collection] = collection
                it[titles.title] = usedTitle
            }
            contentTitles.forEach { title ->
                titles.insertIgnore {
                    it[titles.collection] = collection
                    it[titles.title] = title
                }
            }
        }
    }

    private fun storeAndGetGenres(genres: List<String>): String? {
        return withTransaction(getStoreDatabase()) {
            val gq = GenreQuery( *genres.toTypedArray() )
            gq.insertAndGetIds()
            gq.getIds().joinToString(",")
        }
    }

    private fun storeCatalog(metadata: MetadataDto, videoDetails: VideoDetails, videoFile: String, genres: String?): Int? {
        val precreatedCatalogQuery = CatalogQuery(
                title = metadata.title,
                cover = metadata.cover?.cover,
                type = metadata.type,
                collection = metadata.collection,
                genres = genres
            )

        val result = when (videoDetails.type) {
            "serie" -> {
                val serieInfo = videoDetails.serieInfo ?: throw RuntimeException("SerieInfo missing in VideoDetails for Serie! $videoFile")
                executeOrException {
                    precreatedCatalogQuery.insertWithSerie(
                        episodeTitle = serieInfo.episodeTitle ?: "",
                        videoFile = videoFile,
                        episode = serieInfo.episodeNumber,
                        season = serieInfo.seasonNumber
                    )
                }
            }
            "movie" -> {
                executeOrException {
                    precreatedCatalogQuery.insertWithMovie(videoFile)
                }
            }
            else -> throw RuntimeException("${videoDetails.type} is not supported!")
        }
        val ignoreException = result?.cause is SQLIntegrityConstraintViolationException && (result as ExposedSQLException).errorCode == 1062
        return if (result == null || ignoreException ) {
            return withTransaction(getStoreDatabase()) {
                precreatedCatalogQuery.getId()
            }
        } else null
    }


}