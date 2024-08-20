package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.data.*
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.getStoreDatabase
import no.iktdev.eventi.database.executeOrException
import no.iktdev.eventi.database.executeWithStatus
import no.iktdev.eventi.database.withTransaction
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.StartOperationEvents
import no.iktdev.mediaprocessing.shared.common.contract.dto.SubtitleFormats
import no.iktdev.mediaprocessing.shared.common.contract.reader.*
import no.iktdev.streamit.library.db.query.CatalogQuery
import no.iktdev.streamit.library.db.query.GenreQuery
import no.iktdev.streamit.library.db.query.SubtitleQuery
import no.iktdev.streamit.library.db.query.SummaryQuery
import no.iktdev.streamit.library.db.tables.catalog
import no.iktdev.streamit.library.db.tables.titles
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.sql.SQLIntegrityConstraintViolationException

@Service
class CompletedTaskListener: CoordinatorEventListener() {
    val log = KotlinLogging.logger {}

    var doNotProduceComplete = System.getenv("DISABLE_COMPLETE").toBoolean() ?: false

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    override fun onReady() {
        super.onReady()
        if (doNotProduceComplete) {
            log.warn { "DoNotProduceComplete is set!\n\tNo complete event will be triggered!\n\tTo enable production of complete vents, remove this line in your environment: \"DISABLE_COMPLETE\"" }
        }
    }

    override fun shouldIHandleFailedEvents(incomingEvent: Event): Boolean {
        return true
    }


    @Autowired
    override var coordinator: Coordinator? = null

    override val produceEvent: Events = Events.EventMediaProcessCompleted
    override val listensForEvents: List<Events> = listOf(
        Events.EventWorkDownloadCoverPerformed,
        Events.EventWorkConvertPerformed,
        Events.EventWorkEncodePerformed,
        Events.EventWorkExtractPerformed
    )

    /**
     * Checks whether it requires encode or extract or both, and it has created events with args
     */
    fun req1(started: MediaProcessStartEvent, events: List<Event>): Boolean {
        val encodeFulfilledOrSkipped = if (started.data?.operations?.contains(StartOperationEvents.ENCODE) == true) {
            events.any { it.eventType == Events.EventMediaParameterEncodeCreated }
        } else true

        val extractFulfilledOrSkipped = if (started.data?.operations?.contains(StartOperationEvents.EXTRACT) == true) {
            events.any { it.eventType == Events.EventMediaParameterExtractCreated }
        } else true

        if (!encodeFulfilledOrSkipped || !extractFulfilledOrSkipped) {
            return false
        } else return true
    }

    /**
     * Checks whether work that was supposed to be created has been created.
     * Checks if all subtitles that can be processed has been created if convert is set.
     */
    fun req2(operations: List<StartOperationEvents>, events: List<Event>): Boolean {
        if (StartOperationEvents.ENCODE in operations) {
            val encodeParamter = events.find { it.eventType == Events.EventMediaParameterEncodeCreated }?.az<EncodeArgumentCreatedEvent>()
            val encodeWork = events.find { it.eventType == Events.EventWorkEncodeCreated }
            if (encodeParamter?.isSuccessful() == true && (encodeWork == null))
                return false
        }

        val extractParamter = events.find { it.eventType == Events.EventMediaParameterExtractCreated }?.az<ExtractArgumentCreatedEvent>()
        val extractWork = events.filter { it.eventType == Events.EventWorkExtractCreated }
        if (StartOperationEvents.EXTRACT in operations) {
            if (extractParamter?.isSuccessful() == true && extractParamter.data?.size != extractWork.size)
                return false
        }

        if (StartOperationEvents.CONVERT in operations) {
            val convertWork = events.filter { it.eventType == Events.EventWorkConvertCreated }

            val supportedSubtitleFormats = SubtitleFormats.entries.map { it.name }
            val eventsSupportsConvert = extractWork.filter { it.data is ExtractArgumentData }
                .filter { (it.dataAs<ExtractArgumentData>()?.outputFile?.let { f -> File(f).extension.uppercase() } in supportedSubtitleFormats) }

            if (convertWork.size != eventsSupportsConvert.size)
                return false
        }

        return true
    }

    /**
     * Checks whether all work that has been created has been completed
     */
    fun req3(operations: List<StartOperationEvents>, events: List<Event>): Boolean {
        if (StartOperationEvents.ENCODE in operations) {
            val encodeWork = events.filter { it.eventType == Events.EventWorkEncodeCreated }
            val encodePerformed = events.filter { it.eventType == Events.EventWorkEncodePerformed }
            if (encodePerformed.size < encodeWork.size)
                return false
        }

        if (StartOperationEvents.EXTRACT in operations) {
            val extractWork = events.filter { it.eventType == Events.EventWorkExtractCreated }
            val extractPerformed = events.filter { it.eventType == Events.EventWorkExtractPerformed }
            if (extractPerformed.size < extractWork.size)
                return false
        }

        if (StartOperationEvents.CONVERT in operations) {
            val convertWork = events.filter { it.eventType == Events.EventWorkConvertCreated }
            val convertPerformed = events.filter { it.eventType == Events.EventWorkConvertPerformed }
            if (convertPerformed.size < convertWork.size)
                return false
        }

        return true
    }

    /**
     * Checks if metadata has cover, if so, 2 events are expected
     */
    fun req4(events: List<Event>): Boolean {
        val metadata = events.find { it.eventType == Events.EventMediaMetadataSearchPerformed }
        if (metadata?.isSuccessful() != true) {
            return true
        }

        val hasCover = metadata.dataAs<pyMetadata>()?.cover != null
        if (hasCover == false) {
            return true
        }

        if (events.any { it.eventType == Events.EventMediaReadOutCover } && events.any { it.eventType == Events.EventWorkDownloadCoverPerformed }) {
            return true
        }
        return false
    }


    override fun isPrerequisitesFulfilled(incomingEvent: Event, events: List<Event>): Boolean {
        val started = events.find { it.eventType == Events.EventMediaProcessStarted }?.az<MediaProcessStartEvent>()
        if (started == null) {
            log.info { "No Start event" }
            return false
        }
        val viableEvents = events.filter { it.isSuccessful() }


        if (!req1(started, events)) {
            //log.info { "${this::class.java.simpleName} Failed Req1" }
            return false
        }

        if (!req2(started.data?.operations ?: emptyList(), viableEvents)) {
            //log.info { "${this::class.java.simpleName} Failed Req2" }
            return false
        }

        if (!req3(started.data?.operations ?: emptyList(), events)) {
            //log.info { "${this::class.java.simpleName} Failed Req3" }
            return false
        }

        if (!req4(events)) {
            log.info { "${this::class.java.simpleName} Failed Req4" }
            return false
        }


        return super.isPrerequisitesFulfilled(incomingEvent, events)
    }

    fun getMetadata(events: List<Event>): MetadataDto? {
        val baseInfo = events.find { it.eventType == Events.EventMediaReadBaseInfoPerformed }?.az<BaseInfoEvent>()
        val mediaInfo = events.find { it.eventType == Events.EventMediaReadOutNameAndType }?.az<MediaOutInformationConstructedEvent>()
        val metadataInfo = events.find { it.eventType == Events.EventMediaMetadataSearchPerformed }?.az<MediaMetadataReceivedEvent>()
        val coverInfo = events.find { it.eventType == Events.EventWorkDownloadCoverPerformed }?.az<MediaCoverDownloadedEvent>()
        val coverTask = events.find { it.eventType == Events.EventMediaReadOutCover }?.az<MediaCoverInfoReceivedEvent>()

        if (baseInfo == null) {
            log.info { "Cant find BaseInfoEvent on ${Events.EventMediaReadBaseInfoPerformed}" }
            return null
        }

        if (mediaInfo == null) {
            log.info { "Cant find MediaOutInformationConstructedEvent on ${Events.EventMediaReadOutNameAndType}" }
            return null
        }

        if (metadataInfo == null) {
            log.info { "Cant find MediaMetadataReceivedEvent on ${Events.EventMediaMetadataSearchPerformed}" }
            return null
        }

        if (coverTask?.isSkipped() == false && coverInfo == null) {
            log.info { "Cant find MediaCoverDownloadedEvent on ${Events.EventWorkDownloadCoverPerformed}" }
        }

        val mediaInfoData = mediaInfo.data?.toValueObject()
        val baseInfoData = baseInfo.data
        val metadataInfoData = metadataInfo.data


        val collection = mediaInfo.data?.outDirectory?.let { File(it).name } ?: baseInfoData?.title

        val coverFileName = coverInfo?.data?.absoluteFilePath?.let {
            File(it).name
        }

        return MetadataDto(
            title = mediaInfoData?.title ?: baseInfoData?.title ?: metadataInfoData?.title ?: return null,
            collection = collection ?: return null,
            cover = coverFileName,
            type = metadataInfoData?.type ?: mediaInfoData?.type ?: return null,
            summary = metadataInfoData?.summary?.filter {it.summary != null }?.map { SummaryInfo(language = it.language, summary = it.summary!! ) } ?: emptyList(),
            genres = metadataInfoData?.genres ?: emptyList(),
            titles = (metadataInfoData?.altTitle ?: emptyList()) + listOfNotNull(mediaInfoData?.title, baseInfoData?.title)
        )
    }

    fun getGenres(events: List<Event>): List<String> {
        val metadataInfo = events.find { it.eventType == Events.EventMediaMetadataSearchPerformed }?.az<MediaMetadataReceivedEvent>()
        return metadataInfo?.data?.genres ?: emptyList()
    }

    fun getSubtitles(metadataDto: MetadataDto?, events: List<Event>): List<SubtitlesDto> {
        val extracted = events.filter { it.eventType == Events.EventWorkExtractPerformed }.mapNotNull { it.dataAs<ExtractedData>() }
        val converted = events.filter { it.eventType == Events.EventWorkConvertPerformed }.mapNotNull { it.dataAs<ConvertedData>() }

        val outFiles = extracted.map { it.outputFile } + converted.flatMap { it.outputFiles }

        return outFiles.map {
            val subtitleFile = File(it)
            SubtitlesDto(
                collection =  metadataDto?.collection ?: subtitleFile.parentFile.parentFile.name,
                language = subtitleFile.parentFile.name,
                subtitleFile = subtitleFile.name,
                format = subtitleFile.extension.uppercase(),
                associatedWithVideo = subtitleFile.nameWithoutExtension,
            )
        }
    }

    fun getVideo(events: List<Event>): VideoDetails? {
        val mediaInfo = events.find { it.eventType == Events.EventMediaReadOutNameAndType }?.az<MediaOutInformationConstructedEvent>()
        val encoded = events.find { it.eventType == Events.EventWorkEncodePerformed }?.dataAs<EncodedData>()?.outputFile
        if (encoded == null) {
            log.warn { "No encode no video details!" }
            return null
        }

        val proper = mediaInfo?.data?.toValueObject() ?: return null

        val details = VideoDetails(
            type = proper.type,
            fileName = File(encoded).name,
            serieInfo = if (proper !is EpisodeInfo) null else SerieInfo(
                episodeTitle = proper.episodeTitle,
                episodeNumber = proper.episode,
                seasonNumber = proper.season,
                title = proper.title
            )
        )
        return details
    }

    fun storeSubtitles(subtitles: List<SubtitlesDto>) {
        subtitles.forEach { subtitle ->
            subtitle to executeWithStatus(getStoreDatabase()) {
                SubtitleQuery(
                    collection = subtitle.collection,
                    associatedWithVideo = subtitle.associatedWithVideo,
                    language = subtitle.language,
                    format = subtitle.format,
                    file = subtitle.subtitleFile
                ).insert()
            }
        }
    }


    fun storeTitles(usedTitle: String,  metadata: MetadataDto) {
        try {
            withTransaction(getStoreDatabase()) {
                titles.insertIgnore {
                    it[masterTitle] = metadata.collection
                    it[title] = NameHelper.normalize(usedTitle)
                    it[type] = 1
                }
                titles.insertIgnore {
                    it[masterTitle] = usedTitle
                    it[title] = NameHelper.normalize(usedTitle)
                    it[type] = 2
                }
                metadata.titles.forEach { title ->
                    titles.insertIgnore {
                        it[masterTitle] = usedTitle
                        it[titles.title] = title
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun storeMetadata(catalogId: Int, metadata: MetadataDto) {
        if (!metadata.cover.isNullOrBlank()) {
            withTransaction(getStoreDatabase()) {
                val storedCatalogCover = catalog.select {
                    (catalog.id eq catalogId)
                }.map { it[catalog.cover] }.firstOrNull()
                if (storedCatalogCover.isNullOrBlank()) {
                    catalog.update({
                        catalog.id eq catalogId
                    }) {
                        it[catalog.cover] = metadata.cover
                    }
                }
            }
        }


        metadata.summary.forEach {
            val result = executeOrException(getStoreDatabase().database) {
                SummaryQuery(
                    cid = catalogId,
                    language = it.language,
                    description = it.summary
                ).insert()
            }
            val ignoreException = result?.cause is SQLIntegrityConstraintViolationException && (result as ExposedSQLException).errorCode == 1062
            if (!ignoreException) {
                result?.printStackTrace()
            }
        }
    }

    fun storeAndGetGenres(genres: List<String>): String? {
        return withTransaction(getStoreDatabase()) {
            val gq = GenreQuery( *genres.toTypedArray() )
            gq.insertAndGetIds()
            gq.getIds().joinToString(",")
        }
    }

    fun storeCatalog(metadata: MetadataDto, videoDetails: VideoDetails, genres: String?): Int? {
        val precreatedCatalogQuery = CatalogQuery(
            title = NameHelper.normalize(metadata.title),
            cover = metadata.cover,
            type = metadata.type,
            collection = metadata.collection,
            genres = genres
        )

        val result = when (videoDetails.type) {
            "serie" -> {
                val serieInfo = videoDetails.serieInfo ?: throw RuntimeException("SerieInfo missing in VideoDetails for Serie! ${videoDetails.fileName}")
                executeOrException {
                    precreatedCatalogQuery.insertWithSerie(
                        episodeTitle = serieInfo.episodeTitle ?: "",
                        videoFile = videoDetails.fileName,
                        episode = serieInfo.episodeNumber,
                        season = serieInfo.seasonNumber
                    )
                }
            }
            "movie" -> {
                executeOrException {
                    precreatedCatalogQuery.insertWithMovie(videoDetails.fileName)
                }
            }
            else -> throw RuntimeException("${videoDetails.type} is not supported!")
        }
        val ignoreException = result?.cause is SQLIntegrityConstraintViolationException && (result as ExposedSQLException).errorCode == 1062
        return withTransaction(getStoreDatabase()) {
            precreatedCatalogQuery.getId()
        }
    }

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val result = super.shouldIProcessAndHandleEvent(incomingEvent, events)
        return result
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume() ?: return
        active = true

        val metadata = getMetadata(events)
        val genres = getGenres(events)
        val subtitles = getSubtitles(metadata, events)
        val video = getVideo(events)


        val storedGenres = storeAndGetGenres(genres)
        val catalogId = if (metadata != null && video != null) {
            storeCatalog(metadata, video, storedGenres)
        } else null



        storeSubtitles(subtitles)
        metadata?.let {
            storeTitles(metadata = metadata, usedTitle = metadata.title)
            catalogId?.let { id ->
                storeMetadata(catalogId = id, metadata = it)
            }
        }

        if (!doNotProduceComplete) {
            onProduceEvent(MediaProcessCompletedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = CompletedEventData(
                    events.map { it.eventId() }
                )
            ))
        }



        active = false
    }

}