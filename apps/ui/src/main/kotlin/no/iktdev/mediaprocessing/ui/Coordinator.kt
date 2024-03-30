package no.iktdev.mediaprocessing.ui

import no.iktdev.mediaprocessing.shared.common.CoordinatorBase
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.mediaprocessing.ui.coordinator.PersistentEventBasedMessageListener
import no.iktdev.mediaprocessing.ui.dto.EventSummarySubItem
import no.iktdev.mediaprocessing.ui.dto.SummaryState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class Coordinator(@Autowired private val template: SimpMessagingTemplate?) : CoordinatorBase<PersistentMessage, PersistentEventBasedMessageListener>() {
    override val listeners = PersistentEventBasedMessageListener()
    val dbReader = PersistentDataReader(getEventsDatabase())

    override fun onCoordinatorReady() {

    }

    override fun onMessageReceived(event: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) {

    }

    override fun createTasksBasedOnEventsAndPersistence(
        referenceId: String,
        eventId: String,
        messages: List<PersistentMessage>
    ) {
    }

    fun readAllEvents() {
        val messages = persistentReader.getAllMessages()
    }

    fun readAllProcesserEvents() {
        val messages = persistentReader.getProcessEvents()
    }


    @Scheduled(fixedDelay = (5_000))
    fun refreshDatabaseData() {

    }

    private fun getCurrentStateFromProcesserEvents(events: List<PersistentProcessDataMessage>): Map<String, EventSummarySubItem> {
        return events.associate {
            it.event.event to EventSummarySubItem(
                eventId = it.eventId,
                status = if (it.consumed) SummaryState.Completed else if (it.claimed) SummaryState.Working else SummaryState.Pending
            )
        }
    }

    private fun getCurrentState(events: List<PersistentMessage>, processes: Map<String, EventSummarySubItem>): SummaryState {
        val stored = events.findLast { it.event == KafkaEvents.EVENT_COLLECT_AND_STORE }
        val started = events.findLast { it.event == KafkaEvents.EVENT_MEDIA_PROCESS_STARTED }
        val completedMediaEvent = events.findLast { it.event == KafkaEvents.EVENT_MEDIA_PROCESS_COMPLETED }
        val completedRequestEvent = events.findLast { it.event == KafkaEvents.EVENT_REQUEST_PROCESS_COMPLETED }

        if (stored != null && stored.data.isSuccess()) {
            return SummaryState.Completed
        }

        if (completedMediaEvent?.data.isSuccess() || completedRequestEvent?.data.isSuccess()) {
            return SummaryState.AwaitingStore
        }
        if (processes.values.all { it.status == SummaryState.Completed }) {
            return SummaryState.AwaitingStore
        } else if (processes.values.any { it.status == SummaryState.Working }) {
            return SummaryState.Working
        } else if (processes.values.any { it.status == SummaryState.Pending }) {
            return SummaryState.Pending
        }

        val workPrepared = events.filter { it.event in listOf(
            KafkaEvents.EVENT_WORK_EXTRACT_CREATED,
            KafkaEvents.EVENT_WORK_CONVERT_CREATED,
            KafkaEvents.EVENT_WORK_ENCODE_CREATED
        ) }
        if (workPrepared.isNotEmpty()) {
            return SummaryState.Pending
        }

        if (started != null && (started.data as MediaProcessStarted).type == ProcessType.MANUAL) {
            return SummaryState.AwaitingConfirmation
        }

        val perparation = events.filter { it.event in listOf(
            KafkaEvents.EVENT_MEDIA_EXTRACT_PARAMETER_CREATED,
            KafkaEvents.EVENT_MEDIA_ENCODE_PARAMETER_CREATED,
        ) }
        if (perparation.isNotEmpty()) {
            return SummaryState.Preparing
        }

        // EVENT_MEDIA_METADATA_SEARCH_PERFORMED


        return SummaryState.Started
    }

    fun buildSummaries() {
        val messages = persistentReader.getAllMessages()

    }

}