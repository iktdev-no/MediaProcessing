package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.data.dataAs
import no.iktdev.eventi.data.eventId
import no.iktdev.eventi.data.referenceId
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.EncodeWorkArgumentsMapping
import no.iktdev.mediaprocessing.shared.common.Preference
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class EncodeWorkArgumentsTaskListener: CoordinatorEventListener() {
    val log = KotlinLogging.logger {}

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null

    override val produceEvent: Events = Events.EncodeParameterCreated

    override val listensForEvents: List<Events> = listOf(
        Events.StreamParsed,
        Events.ReadOutNameAndType
    )
    val preference = Preference.getPreference()

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val state = super.shouldIProcessAndHandleEvent(incomingEvent, events)
        val eventType = events.map { it.eventType }

        val startOperation = events.findFirstOf(Events.ProcessStarted)?.dataAs<StartEventData>() ?: return false
        if (startOperation.operations.none { it == OperationEvents.ENCODE }) {
            return false
        }

        return state && eventType.containsAll(listensForEvents)
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }
        val started = events.find { it.eventType == Events.ProcessStarted }?.az<MediaProcessStartEvent>() ?: return

        active = true

        val streamsParsed = events.findEventOf<MediaFileStreamsParsedEvent>()
        val streams = streamsParsed?.data
        if (streams == null) {
            active = false
            log.error { "No Streams found for event ${streamsParsed?.metadata?.eventId} with referenceId ${event.metadata.referenceId}" }
            return
        }

        val mediaInfoEvent = events.findEventOf<MediaOutInformationConstructedEvent>()
        val mediaInfo = mediaInfoEvent?.data
        val mediaInfoData = mediaInfo?.toValueObject()
        if (mediaInfo == null) {
            active = false
            log.error { "No media info data was provided for event ${mediaInfoEvent?.eventId()} with referenceId ${event.referenceId()}" }
            return
        } else if (mediaInfoData == null) {
            active = false
            log.error { "Media info data was provided but could not be converted to proper value object for event ${mediaInfoEvent?.eventId()} with referenceId ${event.referenceId()}" }
            return
        }

        val inputFile = started.data?.file
        if (inputFile == null) {
            active = false
            log.error { "No input file was provided for the start event ${started.metadata.eventId} with referenceId ${event.referenceId()}" }
            return
        }
        val mapper = EncodeWorkArgumentsMapping(
            inputFile = inputFile,
            outFileFullName = mediaInfoData.fullName,
            streams = streams,
            preference = preference.encodePreference
        )

        val result = mapper.getArguments()
        if (result == null) {
            onProduceEvent(EncodeArgumentCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Failed, getProducerName())
            ))
        } else {
            onProduceEvent(EncodeArgumentCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = result
            ))
        }
        active = false
    }

    override fun produceFailure(incomingEvent: Event) {
        onProduceEvent(EncodeArgumentCreatedEvent(
            metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed, getProducerName()),
        ))
    }
}