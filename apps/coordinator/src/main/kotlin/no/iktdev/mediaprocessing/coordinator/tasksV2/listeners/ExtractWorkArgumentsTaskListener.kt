package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.ExtractWorkArgumentsMapping
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtractWorkArgumentsTaskListener: CoordinatorEventListener() {
    val log = KotlinLogging.logger {}

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.ExtractParameterCreated
    override val listensForEvents: List<Events> = listOf(
        Events.StreamParsed,
        Events.ReadOutNameAndType
    )

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val startEvent = events.findFirstEventOf<MediaProcessStartEvent>()
        val hasExtract = startEvent?.data?.operations?.contains(OperationEvents.EXTRACT) ?: false

        val state = super.shouldIProcessAndHandleEvent(incomingEvent, events)
        val eventType = events.map { it.eventType }
        return hasExtract && state && eventType.containsAll(listensForEvents)
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }
        active = true
        val started = events.find { it.eventType == Events.ProcessStarted }?.az<MediaProcessStartEvent>() ?: return
        if (started.data == null || started.data?.operations?.contains(OperationEvents.EXTRACT) == false) {
            active = false
            return
        }
        val streams = events.find { it.eventType == Events.StreamParsed }?.az<MediaFileStreamsParsedEvent>()?.data
        if (streams == null) {
            active = false
            return
        }

        val mediaInfo = events.find { it.eventType == Events.ReadOutNameAndType }?.az<MediaOutInformationConstructedEvent>()
        if (mediaInfo?.data == null) {
            active = false
            return
        }
        val mediaInfoData = mediaInfo.data?.toValueObject()
        if (mediaInfoData == null) {
            active = false
            return
        }

        val inputFile = started.data?.file
        if (inputFile == null) {
            active = false
            return
        }

        val mapper = ExtractWorkArgumentsMapping(
            inputFile = inputFile,
            outFileFullName = mediaInfoData.fullName,
            streams = streams
        )

        val result = mapper.getArguments()
        if (result.isEmpty()) {
            onProduceEvent(ExtractArgumentCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Skipped, getProducerName())
            ))
        } else {
            onProduceEvent(ExtractArgumentCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = result
            ))
        }
        active = false
    }

    override fun produceFailure(incomingEvent: Event) {
        onProduceEvent(ExtractArgumentCreatedEvent(
            metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed, getProducerName()),
            data = null
        ))
    }
}