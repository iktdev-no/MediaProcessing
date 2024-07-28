package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.isSuccessful
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.mediaprocessing.shared.common.parsing.Regexes
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CoverFromMetadataTaskListener: CoordinatorEventListener() {
    val log = KotlinLogging.logger {}

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null

    override val produceEvent: Events = Events.EventMediaReadOutCover
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaMetadataSearchPerformed
    )

    override fun isPrerequisitesFulfilled(incomingEvent: Event, events: List<Event>): Boolean {
        return (events.any { it.eventType == Events.EventMediaReadOutNameAndType && it.isSuccessful() })
    }

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val state = super.shouldIProcessAndHandleEvent(incomingEvent, events)
        if (!state) {
            return false
        }
        if (!incomingEvent.isSuccessful())
            return false
        return incomingEvent.eventType in listensForEvents
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }
        active = true

        val baseInfo = events.find { it.eventType == Events.EventMediaReadBaseInfoPerformed }?.az<BaseInfoEvent>()?.data
        if (baseInfo == null) {
            log.info { "No base info" }
            active = false
            return
        }

        val metadataEvent = if (event.eventType == Events.EventMediaMetadataSearchPerformed) event else events.findLast { it.eventType == Events.EventMediaMetadataSearchPerformed }
        val metadata = metadataEvent?.az<MediaMetadataReceivedEvent>()?.data
            ?: return
        val mediaOutInfo = events.find { it.eventType == Events.EventMediaReadOutNameAndType }?.az<MediaOutInformationConstructedEvent>()?.data
        if (mediaOutInfo == null) {
            log.info { "No Media out info" }
            active = false
            return
        }
        val videoInfo = mediaOutInfo.toValueObject()

        var coverTitle = metadata.title ?: videoInfo?.title ?: baseInfo.title
        coverTitle = Regexes.illegalCharacters.replace(coverTitle, " - ")
        coverTitle = Regexes.trimWhiteSpaces.replace(coverTitle, " ")

        val coverUrl = metadata.cover
        val result = if (coverUrl.isNullOrBlank()) {
            log.warn { "No cover available for ${baseInfo.title}" }
            MediaCoverInfoReceivedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Skipped, getProducerName())
            )
        } else {
            MediaCoverInfoReceivedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = CoverDetails(
                    url = coverUrl,
                    outFileBaseName = NameHelper.normalize(coverTitle),
                    outDir = mediaOutInfo.outDirectory,
                )
            )
        }
        onProduceEvent(result)
        active = false
    }
}