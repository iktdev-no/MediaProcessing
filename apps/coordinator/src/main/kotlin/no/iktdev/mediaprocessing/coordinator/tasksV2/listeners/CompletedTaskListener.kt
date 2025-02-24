package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.GsonBuilder
import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.LocalDateTimeAdapter
import no.iktdev.eventi.data.*
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.EventsSummaryMapping
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store.*
import no.iktdev.mediaprocessing.coordinator.tasksV2.validator.CompletionValidator
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.reader.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime

@Service
class CompletedTaskListener : CoordinatorEventListener() {
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

    override val produceEvent: Events = Events.ProcessCompleted
    override val listensForEvents: List<Events> = listOf(
        Events.WorkDownloadCoverPerformed,
        Events.WorkConvertPerformed,
        Events.WorkEncodePerformed,
        Events.WorkExtractPerformed,
        Events.PersistContentPerformed
    )


    override fun isPrerequisitesFulfilled(incomingEvent: Event, events: List<Event>): Boolean {
        val started = events.find { it.eventType == Events.ProcessStarted }?.az<MediaProcessStartEvent>()
        if (started == null) {
            log.info { "No Start event" }
            return false
        }
        val viableEvents = events.filter { it.isSuccessful() }

        if (!CompletionValidator.req1(started, events)) {
            return false
        }

        if (!CompletionValidator.req2(started.data?.operations ?: emptyList(), viableEvents)) {
            return false
        }

        if (!CompletionValidator.req3(started.data?.operations ?: emptyList(), events)) {
            return false
        }

        if (!CompletionValidator.req4(events)) {
            return false
        }

        return super.isPrerequisitesFulfilled(incomingEvent, events)
    }

    fun getVideo(events: List<Event>): VideoDetails? {
        val mediaInfo = events.find { it.eventType == Events.ReadOutNameAndType }?.az<MediaOutInformationConstructedEvent>()
        val encoded = events.find { it.eventType == Events.WorkEncodePerformed }?.dataAs<EncodedData>()?.outputFile
        if (encoded == null) {
            log.warn { "No encode no video details!" }
            return null
        }

        val proper = mediaInfo?.data?.toValueObject() ?: run {
            log.error { "Unable to get media object from data" }
            return null
        }

        return VideoDetails(
            type = proper.type,
            fileName = File(encoded).name,
            serieInfo = if (proper !is EpisodeInfo) null else SerieInfo(
                episodeTitle = proper.episodeTitle,
                episodeNumber = proper.episode,
                seasonNumber = proper.season,
                title = proper.title
            )
        )
    }

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val result = super.shouldIProcessAndHandleEvent(incomingEvent, events)
        return result
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume() ?: return
        active = true

        val mediaInfo: ComposedMediaInfo = composeMediaInfo(events) ?: run {
            log.error { "Unable to compose media info for ${event.referenceId()}" }
            return
        }

        val existingTitles = ContentTitleStore.findMasterTitles(mediaInfo.titles)

        val usableCollection: String = if (existingTitles.isNotEmpty())
            ContentCatalogStore.getCollectionByTitleAndType(mediaInfo.type, existingTitles) ?: run {
                log.warn { "Did not receive collection based on titles provided in list ${existingTitles.joinToString(",")}, falling back to fallbackCollection: ${mediaInfo.fallbackCollection}" }
                mediaInfo.fallbackCollection
            } else mediaInfo.fallbackCollection

        val genreIdsForCatalog = ContentGenresStore.storeAndGetIds(mediaInfo.genres)

        val persistedContent: PersistedContent? = events.find { it.eventType == Events.PersistContentPerformed }?.az<PersistedContentEvent>()?.data
        if (persistedContent == null) {
            log.error { "PersistedContent is null! can't continue" }
            return
        }

        val completedData = CompletedData(
            eventIdsCollected = events.map { it.eventId() },
            metadataStored = MetadataStored(
                title = mediaInfo.title,
                titles = mediaInfo.titles,
                type = mediaInfo.type,
                cover = persistedContent.cover?.storeDestinationFileName?.let { File(it).name },
                collection = usableCollection,
                summary = mediaInfo.summaries,
                foundTitles = existingTitles,
                genres = mediaInfo.genres,
                genreIds = genreIdsForCatalog
            )
        )

        completedData.metadataStored.let { meta ->
            val catalogId = ContentCatalogStore.storeCatalog(
                title = meta.title,
                collection = meta.collection,
                type = meta.type,
                cover = meta.cover,
                genres = meta.genreIds
            )
            catalogId?.let { id ->
                meta.summary.forEach { summary ->
                    ContentMetadataStore.storeSummary(id, summary)
                }
            }
            ContentTitleStore.store(meta.title, meta.titles)
        }


        val videoInfo = getVideo(events)
        if (videoInfo != null) {
            ContentCatalogStore.storeMedia(
                title = completedData.metadataStored.title,
                collection = completedData.metadataStored.collection,
                type = completedData.metadataStored.type,
                videoDetails = videoInfo
            )
        } else {
            log.info { "VideoInfo is null" }
        }


        try {
            persistedContent.subtitles.let { subtitles ->
                subtitles.map {
                    ContentSubtitleStore.storeSubtitles(
                        collection = completedData.metadataStored.collection,
                        destinationFile = File(it.storeDestinationFileName)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ProcessedFileStore.store(
            mediaInfo.title,
            events,
            EventsSummaryMapping().map(events)
        )

        if (!doNotProduceComplete) {
            onProduceEvent(MediaProcessCompletedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = completedData
            ))
        } else {
            log.warn { "Do not produce complete is enabled!" }
        }

        active = false
    }

    internal data class ComposedMediaInfo(
        val title: String,
        val fallbackCollection: String,

        val titles: List<String>,
        val type: String,
        val summaries: List<SummaryInfo>,
        val genres: List<String>
    )

    private fun composeMediaInfo(events: List<Event>): ComposedMediaInfo? {
        val baseInfo =
            events.find { it.eventType == Events.ReadBaseInfoPerformed }?.az<BaseInfoEvent>()?.let {
                it.data
            } ?: run {
                log.info { "Cant find BaseInfoEvent on ${Events.ReadBaseInfoPerformed}" }
                return null
            }
        val metadataInfo = getMetadata(events)
        val mediaInfo: MediaInfo = events.find { it.eventType == Events.ReadOutNameAndType }
            ?.az<MediaOutInformationConstructedEvent>()?.let {
                it.data?.toValueObject()
            } ?: run {
            log.info { "Cant find MediaOutInformationConstructedEvent on ${Events.ReadOutNameAndType}" }
            return null
        }

        val summaries = metadataInfo?.summary?.filter { it.summary != null }
            ?.map { SummaryInfo(language = it.language, summary = it.summary!!) } ?: emptyList()

        val titles: MutableList<String> = mutableListOf(mediaInfo.title)
        metadataInfo?.let {
            titles.addAll(it.altTitle)
            titles.add(it.title)
            titles.add(NameHelper.normalize(it.title))
        }

        return ComposedMediaInfo(
            title = NameHelper.normalize(metadataInfo?.title ?: mediaInfo.title),
            fallbackCollection = baseInfo.title,
            titles = titles,
            type = metadataInfo?.type ?: mediaInfo.type,
            summaries = summaries,
            genres = metadataInfo?.genres ?: emptyList()
        )
    }


    private fun getMetadata(events: List<Event>): pyMetadata? {
        val referenceId = events.firstNotNullOf { it.referenceId() }

        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create()

        //log.info { "Events in complete:\n${gson.toJson(events)}" }

        val metadataFound = events.find { it.eventType == Events.MetadataSearchPerformed }
        if (metadataFound == null) {
            log.warn { "ReferenceId: $referenceId -> ${Events.MetadataSearchPerformed} was not found in events" }
            return null
        }
        val data = metadataFound.az<MediaMetadataReceivedEvent>()
        if (data == null) {
            log.warn { "ReferenceId: $referenceId -> ${Events.MetadataSearchPerformed} does not contain any data.." }
            return null
        }

        return data.data
    }

}