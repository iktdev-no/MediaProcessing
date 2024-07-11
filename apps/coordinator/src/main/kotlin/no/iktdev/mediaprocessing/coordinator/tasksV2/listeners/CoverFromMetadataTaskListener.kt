package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.mediaprocessing.shared.common.parsing.Regexes
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsListenerContract
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CoverFromMetadataTaskListener: CoordinatorEventListener() {
    val log = KotlinLogging.logger {}


    @Autowired
    override var coordinator: Coordinator? = null

    override val produceEvent: Events = Events.EventMediaReadOutCover
    override val listensForEvents: List<Events> = listOf(Events.EventMediaMetadataSearchPerformed)

    override fun onEventsReceived(incomingEvent: Event, events: List<Event>) {
        val baseInfo = events.find { it.eventType == Events.EventMediaReadBaseInfoPerformed }?.az<BaseInfoEvent>()?.data ?: return
        val metadata = events.findLast { it.eventType == Events.EventMediaMetadataSearchPerformed }?.az<MediaMetadataReceivedEvent>()?.data ?: return
        val mediaOutInfo = events.find { it.eventType == Events.EventMediaReadOutNameAndType }?.az<MediaOutInformationConstructedEvent>()?.data ?: return
        val videoInfo = mediaOutInfo.toValueObject()

        var coverTitle = metadata.title ?: videoInfo?.title ?: baseInfo.title
        coverTitle = Regexes.illegalCharacters.replace(coverTitle, " - ")
        coverTitle = Regexes.trimWhiteSpaces.replace(coverTitle, " ")

        val coverUrl = metadata.cover
        val result = if (coverUrl.isNullOrBlank()) {
            log.warn { "No cover available for ${baseInfo.title}" }
            MediaCoverInfoReceivedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Skipped)
            )
        } else {
            MediaCoverInfoReceivedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Success),
                data = CoverDetails(
                    url = coverUrl,
                    outFileBaseName = NameHelper.normalize(coverTitle),
                    outDir = mediaOutInfo.outDirectory,
                )
            )
        }
        onProduceEvent(result)

    }
}