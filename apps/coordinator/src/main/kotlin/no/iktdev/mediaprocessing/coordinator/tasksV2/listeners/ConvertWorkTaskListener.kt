package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.*
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.taskManager
import no.iktdev.mediaprocessing.coordinator.tasksV2.implementations.WorkTaskListener
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.*
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import no.iktdev.mediaprocessing.shared.contract.dto.isOnly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ConvertWorkTaskListener: WorkTaskListener() {
    val log = KotlinLogging.logger {}

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.EventWorkConvertCreated
    override val listensForEvents: List<Events> = listOf(
        Events.EventWorkExtractPerformed
    )

    override fun canProduceMultipleEvents(): Boolean {
        return true
    }
    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        if (!isOfEventsIListenFor(incomingEvent))
            return false
        if (!incomingEvent.isSuccessful() && !shouldIHandleFailedEvents(incomingEvent)) {
            return false
        }
        val producedEvents = events.filter { it.eventType == produceEvent }
        val shouldIHandleAndProduce = producedEvents.none { it.derivedFromEventId() == incomingEvent.eventId() }
        if (shouldIHandleAndProduce) {
            log.info { "Permitting handling of event: ${incomingEvent.dataAs<ExtractedData>()?.outputFile}" }
        }
        return shouldIHandleAndProduce
    }
    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }
        active = true
        if (!canStart(event, events)) {
            active = false
            return
        }

        val file = if (event.eventType == Events.EventWorkExtractPerformed) {
            event.az<ExtractWorkPerformedEvent>()?.data?.outputFile
        } else if (event.eventType == Events.EventMediaProcessStarted) {
            val startEvent = event.az<MediaProcessStartEvent>()?.data
            if (startEvent?.operations?.isOnly(StartOperationEvents.CONVERT) == true) {
                startEvent.file
            } else null
        } else {
            events.find { it.eventType == Events.EventWorkExtractPerformed }
                ?.az<ExtractWorkPerformedEvent>()?.data?.outputFile
        }


        val convertFile = file?.let { File(it) }
        if (convertFile == null || !convertFile.exists()) {
            onProduceEvent(ConvertWorkCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Failed, getProducerName())
            ))
            return
        } else {
            val convertData = ConvertData(
                inputFile = convertFile.absolutePath,
                outputFileName = convertFile.nameWithoutExtension,
                outputDirectory = convertFile.parentFile.absolutePath,
                allowOverwrite = true
            )


            ConvertWorkCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = convertData
            ).also { event ->
                onProduceEvent(event)
                taskManager.createTask(
                    referenceId = event.referenceId(),
                    eventId = event.eventId(),
                    derivedFromEventId = event.derivedFromEventId(),
                    task = TaskType.Convert,
                    data = WGson.gson.toJson(event.data!!),
                    inputFile = event.data!!.inputFile
                )
            }
        }
        active = false
    }
}