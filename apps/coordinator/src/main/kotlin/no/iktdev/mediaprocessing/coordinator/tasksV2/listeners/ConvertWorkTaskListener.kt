package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.*
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.taskManager
import no.iktdev.mediaprocessing.coordinator.tasksV2.implementations.WorkTaskListener
import no.iktdev.mediaprocessing.shared.common.task.TaskType
import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.*
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import no.iktdev.mediaprocessing.shared.common.contract.dto.isOnly
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
    override val produceEvent: Events = Events.ConvertTaskCreated
    override val listensForEvents: List<Events> = listOf(
        Events.ExtractTaskCompleted,
        Events.ProcessStarted
    )

    override fun canProduceMultipleEvents(): Boolean {
        return true
    }
    override fun shouldIProcessAndHandleEvent(incomingEvent: Event, events: List<Event>): Boolean {
        if (!super.shouldIProcessAndHandleEvent(incomingEvent, events)) {
            return false
        }

        if (!incomingEvent.isSuccessful() && !shouldIHandleFailedEvents(incomingEvent)) {
            return false
        }
        val producedEvents = events.filter { it.eventType == produceEvent }
        val shouldIHandleAndProduce = producedEvents.none { it.derivedFromEventId() == incomingEvent.eventId() }

        val extractedEvent = events.findFirstEventOf<ExtractWorkPerformedEvent>()
        if (extractedEvent?.isSuccessful() == true && shouldIHandleAndProduce) {
            log.info { "Permitting handling of event: ${extractedEvent.data?.outputFile}" }

        }

        val startedWithOperations = events.findFirstEventOf<MediaProcessStartEvent>()?.data?.operations ?: return false
        if (startedWithOperations.isOnly(OperationEvents.CONVERT) && shouldIHandleAndProduce) {
            return true
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

        var language: String? = null
        var storeAsFile: String? = null


        val file = if (event.eventType == Events.ExtractTaskCompleted) {
            val foundEvent = event.az<ExtractWorkPerformedEvent>()?.data
            language = foundEvent?.language
            storeAsFile = foundEvent?.storeFileName
            foundEvent?.outputFile
        } else if (event.eventType == Events.ProcessStarted) {
            val startEvent = event.az<MediaProcessStartEvent>()?.data
            if (startEvent?.operations?.isOnly(OperationEvents.CONVERT) == true) {
                startEvent.file
            } else null
        } else {
            events.find { it.eventType == Events.ExtractTaskCompleted }
                ?.az<ExtractWorkPerformedEvent>()?.data?.outputFile
        }


        val convertFile = file?.let { File(it) }
        if (language.isNullOrEmpty()) {
            convertFile?.parentFile?.nameWithoutExtension?.let {
                if (it.length == 3) {
                    language = it.lowercase()
                }
            }
        }


        if (convertFile == null || !convertFile.exists()) {
            onProduceEvent(ConvertWorkCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Failed, getProducerName())
            ))
            return
        } else {
            val convertData = ConvertData(
                language = language ?: "unk",
                inputFile = convertFile.absolutePath,
                outputFileName = convertFile.nameWithoutExtension,
                storeFileName = storeAsFile ?: convertFile.nameWithoutExtension,
                outputDirectory = convertFile.parentFile.absolutePath,
                allowOverwrite = true
            )


            ConvertWorkCreatedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = convertData
            ).also { event ->
                val taskCreatedSuccessfully = taskManager.createTask(
                    referenceId = event.referenceId(),
                    eventId = event.eventId(),
                    derivedFromEventId = event.derivedFromEventId(),
                    task = TaskType.Convert,
                    data = WGson.gson.toJson(event.data!!),
                    inputFile = event.data!!.inputFile
                )

                if (!taskCreatedSuccessfully) {
                    log.error { "Failed to create task for events on referenceId: ${event.referenceId()}" }
                } else {
                    onProduceEvent(event)
                }

            }
        }
        active = false
    }
}