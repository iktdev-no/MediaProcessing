package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
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

    override val produceEvent: Events = Events.EventMediaProcessCompleted
    override val listensForEvents: List<Events> = listOf(
        Events.EventWorkDownloadCoverPerformed,
        Events.EventWorkConvertPerformed,
        Events.EventWorkEncodePerformed,
        Events.EventWorkExtractPerformed
    )


    override fun isPrerequisitesFulfilled(incomingEvent: Event, events: List<Event>): Boolean {
        val started = events.find { it.eventType == Events.EventMediaProcessStarted }?.az<MediaProcessStartEvent>()
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
        val mediaInfo = events.find { it.eventType == Events.EventMediaReadOutNameAndType }
            ?.az<MediaOutInformationConstructedEvent>()
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

        val mover = ContentCompletionMover(usableCollection, events)


        val genreIdsForCatalog = ContentGenresStore.storeAndGetIds(mediaInfo.genres)
        val newCoverPath = mover.moveCover()

        ContentCatalogStore.storeCatalog(
            title = mediaInfo.title,
            collection = usableCollection,
            type = mediaInfo.type,
            cover = newCoverPath?.second?.let { dp -> File(dp).name },
            genres = genreIdsForCatalog,
        )?.also { cid ->
            mediaInfo.summaries.forEach {
                ContentMetadataStore.storeSummary(cid, it)
            }
            ContentTitleStore.store(mediaInfo.title, mediaInfo.titles)
        }



        val newVideoPath = mover.moveVideo()
        try {
            getVideo(events)?.let { video ->
                ContentCatalogStore.storeMedia(
                    title = mediaInfo.title,
                    collection = usableCollection,
                    type = mediaInfo.type,
                    videoDetails = video
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val newSubtitles = mover.moveSubtitles()

        try {
            newSubtitles?.let { subtitles ->
                subtitles.map {
                    ContentSubtitleStore.storeSubtitles(
                        collection = usableCollection,
                        language = it.language,
                        destinationFile = File(it.destination)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val summary = EventsSummaryMapping().map(events)


        ProcessedFileStore.store(
            mediaInfo.title,
            events,
            summary
        )


        if (!doNotProduceComplete) {
            onProduceEvent(MediaProcessCompletedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = CompletedEventData(
                    eventIdsCollected = events.map { it.eventId() },
                    coverMoved = newCoverPath?.let { c -> CoverMoved(c.first, c.second) },
                    videoMoved = newVideoPath?.let { v -> VideoMoved(v.first, v.second) },
                    subtitlesMoved = newSubtitles?.map { s -> SubtitlesMoved(s.source, s.destination) } ?: emptyList()
                )
            ))
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
            events.find { it.eventType == Events.EventMediaReadBaseInfoPerformed }?.az<BaseInfoEvent>()?.let {
                it.data
            } ?: run {
                log.info { "Cant find BaseInfoEvent on ${Events.EventMediaReadBaseInfoPerformed}" }
                return null
            }
        val metadataInfo =
            events.find { it.eventType == Events.EventMediaMetadataSearchPerformed }?.az<MediaMetadataReceivedEvent>()?.data
                ?: run {
                log.info { "Cant find MediaMetadataReceivedEvent on ${Events.EventMediaMetadataSearchPerformed}" }
                null
            }
        val mediaInfo: MediaInfo = events.find { it.eventType == Events.EventMediaReadOutNameAndType }
            ?.az<MediaOutInformationConstructedEvent>()?.let {
                it.data?.toValueObject()
            } ?: run {
            log.info { "Cant find MediaOutInformationConstructedEvent on ${Events.EventMediaReadOutNameAndType}" }
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


}