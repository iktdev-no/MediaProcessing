package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.eventi.data.EventStatus
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

    @Autowired
    override var coordinator: Coordinator? = null
    override val produceEvent: Events = Events.EventWorkConvertCreated
    override val listensForEvents: List<Events> = listOf(
        Events.EventWorkExtractPerformed
    )

    override fun onEventsReceived(incomingEvent: Event, events: List<Event>) {
        if (!canStart(incomingEvent, events)) {
            return
        }

        val file = if (incomingEvent.eventType == Events.EventWorkExtractPerformed) {
            incomingEvent.az<ExtractWorkPerformedEvent>()?.data?.outputFile
        } else if (incomingEvent.eventType == Events.EventMediaProcessStarted) {
            val startEvent = incomingEvent.az<MediaProcessStartEvent>()?.data
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
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed)
            ))
            return
        } else {
            val convertData = ConvertData(
                inputFile = convertFile.absolutePath,
                outputFileName = convertFile.nameWithoutExtension,
                outputDirectory = convertFile.parentFile.absolutePath,
                allowOverwrite = true
            )

            val status = taskManager.createTask(
                referenceId = incomingEvent.referenceId(),
                eventId = incomingEvent.eventId(),
                task = TaskType.Convert,
                derivedFromEventId = incomingEvent.eventId(),
                data = Gson().toJson(convertData),
                inputFile = convertFile.absolutePath
            )

            if (!status) {
                log.error { "Failed to create Convert task on ${incomingEvent.referenceId()}@${incomingEvent.eventId()}" }
                return
            }

            onProduceEvent(ConvertWorkCreatedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Success),
                data = convertData
            ))
        }
    }
}