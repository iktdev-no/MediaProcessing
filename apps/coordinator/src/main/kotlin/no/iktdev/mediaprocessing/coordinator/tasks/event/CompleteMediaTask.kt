package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.Gson
import mu.KotlinLogging
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.coordinator.mapping.ProcessMapping
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents.*
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessCompleted
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CompleteMediaTask(@Autowired override var coordinator: EventCoordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}

    override val producesEvent: KafkaEvents = KafkaEvents.EventMediaProcessCompleted

    override val requiredEvents: List<KafkaEvents> = listOf(
        EventMediaProcessStarted,
        EventMediaReadBaseInfoPerformed,
        EventMediaReadOutNameAndType
    )
    override val listensForEvents: List<KafkaEvents> = KafkaEvents.entries



    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)

        log.info { "${event.referenceId} triggered by ${event.event}" }

        val started = events.lastOrSuccessOf(EventMediaProcessStarted) ?: return null
        if (!started.data.isSuccess()) {
            return null
        }

        val receivedEvents = events.map { it.event }
        // TODO: Add filter in case a metadata request was performed or a cover download was performed. for now, for base functionality, it requires a performed event.




        val ffmpegEvents = listOf(KafkaEvents.EventMediaParameterEncodeCreated, EventMediaParameterExtractCreated)
        if (ffmpegEvents.any { receivedEvents.contains(it) } && events.none { e -> KafkaEvents.isOfWork(e.event) }) {
            return null
        }

        val startedData: MediaProcessStarted? = started.data as MediaProcessStarted?
        if (startedData == null) {
            log.error { "${event.referenceId} contains a started event without proper data object" }
            return null
        }


        val hasEncodeAndIsRequired = if (startedData.operations.contains(StartOperationEvents.ENCODE)) {
            events.any { it.event == EventWorkEncodePerformed }
        } else true

        val hasExtractAndIsRequired = if (startedData.operations.contains(StartOperationEvents.EXTRACT)) {
            events.any { it.event == EventWorkExtractPerformed}
        } else true

        val hasConvertAndIsRequired = if (startedData.operations.contains(StartOperationEvents.CONVERT)) {
            events.any { it.event == EventWorkConvertPerformed }
        } else true

        val missingRequired: MutableMap<StartOperationEvents, Boolean> = mutableMapOf(
            StartOperationEvents.ENCODE to hasEncodeAndIsRequired,
            StartOperationEvents.EXTRACT to hasExtractAndIsRequired,
            StartOperationEvents.CONVERT to hasConvertAndIsRequired
        )

        if (missingRequired.values.any { !it }) {
            log.info { "Waiting for ${missingRequired.entries.filter { !it.value }.map { it.key.name }}" }
            return null
        }

        val ch = CompleteHandler(events)
        val chEvents = ch.getMissingCompletions()
        if (chEvents.isNotEmpty()) {
            log.info { "Waiting for ${chEvents.joinToString(",")}" }
            log.warn { "Waiting report: ${Gson().toJson(chEvents)}" }
            return null
        }



        val mapper = ProcessMapping(events)
        if (mapper.canCollect()) {
            return ProcessCompleted(Status.COMPLETED, event.eventId)
        }
        return null
    }

    class CompleteHandler(val events: List<PersistentMessage>) {
        var report: Map<KafkaEvents, Int> = listOf(
            EventReport.fromEvents(EventWorkEncodeCreated, events),
            EventReport.fromEvents(EventWorkExtractCreated, events),
            EventReport.fromEvents(EventWorkConvertCreated, events),

            EventReport.fromEvents(EventWorkEncodePerformed, events),
            EventReport.fromEvents(EventWorkExtractPerformed, events),
            EventReport.fromEvents(EventWorkConvertPerformed, events)
        ).associate { it.event to it.count }

        fun getMissingCompletions(): List<StartOperationEvents> {
            val missings = mutableListOf<StartOperationEvents>()
            if ((report[EventWorkEncodeCreated]?: 0) > (report[EventWorkEncodePerformed] ?: 0))
                missings.add(StartOperationEvents.ENCODE)
            if ((report[EventWorkExtractCreated] ?: 0) > (report[EventWorkExtractPerformed] ?: 0))
                missings.add(StartOperationEvents.EXTRACT)
            if ((report[EventWorkConvertCreated] ?: 0) > (report[EventWorkConvertPerformed] ?: 0))
                missings.add(StartOperationEvents.CONVERT)
            return missings
        }

        data class EventReport(val event: KafkaEvents, val count: Int) {
            companion object {
                fun fromEvents(event: KafkaEvents, events: List<PersistentMessage>): EventReport {
                    return EventReport(event = event, count = events.filter { it.event == event }.size)
                }
            }
        }
    }
}