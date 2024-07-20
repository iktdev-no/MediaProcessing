package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.tasksV2.mapping.ExtractWorkArgumentsMapping
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsListenerContract
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.*
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ExtractWorkArgumentsTaskListener: CoordinatorEventListener() {
    val log = KotlinLogging.logger {}

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.EventMediaParameterExtractCreated
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaParseStreamPerformed,
        Events.EventMediaReadOutNameAndType
    )

    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        val state = super.shouldIProcessAndHandleEvent(incomingEvent, events)
        val eventType = events.map { it.eventType }
        return state && eventType.containsAll(listensForEvents)
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }
        val started = events.find { it.eventType == Events.EventMediaProcessStarted }?.az<MediaProcessStartEvent>() ?: return
        if (started.data == null || started.data?.operations?.contains(StartOperationEvents.EXTRACT) == false) {
            return
        }
        val streams = events.find { it.eventType == Events.EventMediaParseStreamPerformed }?.az<MediaFileStreamsParsedEvent>()?.data
        if (streams == null) {
            return
        }

        val mediaInfo = events.find { it.eventType == Events.EventMediaReadOutNameAndType }?.az<MediaOutInformationConstructedEvent>()
        if (mediaInfo?.data == null) {
            return
        }
        val mediaInfoData = mediaInfo.data?.toValueObject() ?: return

        val inputFile = started.data?.file ?: return

        val mapper = ExtractWorkArgumentsMapping(
            inputFile = inputFile,
            outFileFullName = mediaInfoData.fullName,
            outFileAbsolutePathFile = mediaInfo.data?.outDirectory?.let { File(it) } ?: return,
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

    }
}