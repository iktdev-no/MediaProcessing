package no.iktdev.mediaprocessing.ui

import no.iktdev.mediaprocessing.shared.common.CoordinatorBase
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MediaProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.VideoInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.mediaprocessing.ui.coordinator.PersistentEventBasedMessageListener
import no.iktdev.mediaprocessing.ui.dto.EventSummary
import no.iktdev.mediaprocessing.ui.dto.EventSummarySubItem
import no.iktdev.mediaprocessing.ui.dto.SummaryState
import no.iktdev.mediaprocessing.ui.socket.EventbasedTopic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class Coordinator(@Autowired private val eventbasedTopic: EventbasedTopic) : CoordinatorBase<PersistentMessage, PersistentEventBasedMessageListener>() {
    override val listeners = PersistentEventBasedMessageListener()

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
        val started = events.findLast { it.event == KafkaEvents.EventMediaProcessStarted }
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
            KafkaEvents.EventWorkExtractCreated,
            KafkaEvents.EventWorkConvertCreated,
            KafkaEvents.EventWorkEncodeCreated
        ) }
        if (workPrepared.isNotEmpty()) {
            return SummaryState.Pending
        }

        if (started != null && (started.data as MediaProcessStarted).type == ProcessType.MANUAL) {
            return SummaryState.AwaitingConfirmation
        }

        val perparation = events.filter { it.event in listOf(
            KafkaEvents.EventMediaParameterExtractCreated,
            KafkaEvents.EventMediaParameterEncodeCreated,
        ) }
        if (perparation.isNotEmpty()) {
            return SummaryState.Preparing
        }

        val analyzed2 = events.findLast { it.event in listOf(KafkaEvents.EventMediaReadOutNameAndType) }
        if (analyzed2 != null) {
            return SummaryState.Analyzing
        }

        val waitingForMeta = events.findLast { it.event == KafkaEvents.EventMediaMetadataSearchPerformed }
        if (waitingForMeta != null) {
            return SummaryState.Metadata
        }

        val analyzed = events.findLast { it.event in listOf(KafkaEvents.EventMediaParseStreamPerformed, KafkaEvents.EventMediaReadBaseInfoPerformed, KafkaEvents.EventMediaReadOutNameAndType) }
        if (analyzed != null) {
            return SummaryState.Analyzing
        }

        val readEvent = events.findLast { it.event == KafkaEvents.EventMediaReadStreamPerformed }
        if (readEvent != null) {
            return SummaryState.Read
        }

        return SummaryState.Started
    }

    fun buildSummaries() {
        val processerMessages = persistentReader.getProcessEvents().groupBy { it.referenceId }
        val messages = persistentReader.getAllMessages()

        val mapped = messages.mapNotNull { it ->
            val referenceId = it.firstOrNull()?.referenceId
            if (referenceId != null) {
                val procM = processerMessages.getOrDefault(referenceId, emptyList())
                val processesStatuses = getCurrentStateFromProcesserEvents(procM)
                val messageStatus = getCurrentState(it, processesStatuses)

                val baseNameEvent = it.lastOrNull {ke -> ke.event == KafkaEvents.EventMediaReadBaseInfoPerformed }?.data.let { data ->
                    if (data is BaseInfoPerformed) data else null
                }
                val mediaNameEvent = it.lastOrNull { ke -> ke.event == KafkaEvents.EventMediaReadOutNameAndType }?.data.let { data ->
                    if (data is VideoInfoPerformed) data else null
                }

                val baseName = if (mediaNameEvent == null) baseNameEvent?.sanitizedName else mediaNameEvent.toValueObject()?.fullName

                EventSummary(
                    referenceId = referenceId,
                    baseName = baseName,
                    collection = mediaNameEvent?.toValueObject()?.title,
                    events = it.map { ke -> ke.event },
                    status = messageStatus,
                    activeEvens = processesStatuses
                )

            } else null
        }


    }

}