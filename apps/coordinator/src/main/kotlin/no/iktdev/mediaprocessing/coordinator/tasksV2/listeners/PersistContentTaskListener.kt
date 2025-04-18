package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.data.*
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.store.*
import no.iktdev.mediaprocessing.coordinator.tasksV2.validator.CompletionValidator
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.reader.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PersistContentTaskListener : CoordinatorEventListener() {
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

    override val produceEvent: Events = Events.PersistContent
    override val listensForEvents: List<Events> = listOf(
        Events.CoverDownloaded,
        Events.ConvertTaskCompleted,
        Events.EncodeTaskCompleted,
        Events.ExtractTaskCompleted
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

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        if (doNotProduceComplete) {
            return false
        }
        val result = super.shouldIProcessAndHandleEvent(incomingEvent, events)
        return result
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume() ?: return
        active = true

        if (doNotProduceComplete) {
            return
        }

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

        val newCoverPath = mover.moveCover()
        val newVideoPath = mover.moveVideo()
        val newSubtitles = mover.moveSubtitles()


        val contentEvent = PersistedContent(
            cover = newCoverPath?.let { PersistedItem(it.first, it.second) },
            video = newVideoPath?.let { PersistedItem(it.first, it.second) },
            subtitles = newSubtitles?.map { PersistedItem(it.source, it.destination) } ?: emptyList()
        )

        onProduceEvent(PersistedContentEvent(
            metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
            data = contentEvent
        ))

        active = false
    }

    override fun produceFailure(incomingEvent: Event) {
        onProduceEvent(
            PersistedContentEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed, getProducerName()),
                data = null
            )
        )
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
            events.find { it.eventType == Events.BaseInfoRead }?.az<BaseInfoEvent>()?.let {
                it.data
            } ?: run {
                log.info { "Cant find BaseInfoEvent on ${Events.BaseInfoRead}" }
                return null
            }
        val metadataInfo =
            events.find { it.eventType == Events.MetadataSearchPerformed }?.az<MediaMetadataReceivedEvent>()?.data
                ?: run {
                log.info { "Cant find MediaMetadataReceivedEvent on ${Events.MetadataSearchPerformed}" }
                null
            }
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


}