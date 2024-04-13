package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.processer.coordination.PersistentEventProcessBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.CoordinatorBase
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.NotificationOfDeletionPerformed
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@EnableScheduling
class Coordinator(): CoordinatorBase<PersistentProcessDataMessage, PersistentEventProcessBasedMessageListener>() {
    private val log = KotlinLogging.logger {}
    val io = Coroutines.io()
    override val listeners = PersistentEventProcessBasedMessageListener()

    private val coordinatorEventListeners: MutableList<CoordinatorEvents> = mutableListOf()
    fun getRegisteredEventListeners() = coordinatorEventListeners.toList()
    fun addCoordinatorEventListener(listener: CoordinatorEvents) {
        coordinatorEventListeners.add(listener)
    }
    fun removeCoordinatorEventListener(listener: CoordinatorEvents) {
        coordinatorEventListeners.remove(listener)
    }

    override fun createTasksBasedOnEventsAndPersistence(
        referenceId: String,
        eventId: String,
        messages: List<PersistentProcessDataMessage>
    ) {
        val triggered = messages.find { it.eventId == eventId }
        if (triggered == null) {
            log.error { "Could not find $eventId in provided messages" }
            return
        }
        listeners.forwardEventMessageToListeners(triggered, messages)
    }

    override fun onCoordinatorReady() {
        readAllAvailableInQueue()
    }

    override fun onMessageReceived(event: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) {
        if (!acceptEvents.contains(event.key)) {
            return
        }
        if (event.key == KafkaEvents.EventNotificationOfWorkItemRemoval) {
            handleDeletionOfEvents(event)
            return
        }

        val success = eventManager.setProcessEvent(event.key, event.value)
        if (!success) {
            log.error { "Unable to store message event: ${event.key.event} with eventId ${event.value.eventId} with referenceId ${event.value.referenceId} in database ${getEventsDatabase().database}!" }
        } else {
            io.launch {
                delay(500)
                readAllMessagesFor(event.value.referenceId, event.value.eventId)
            }
        }
    }

    private fun handleDeletionOfEvents(kafkaPayload: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) {
        if (kafkaPayload.value.data is NotificationOfDeletionPerformed) {
            val data = kafkaPayload.value.data as NotificationOfDeletionPerformed
            if (data.deletedEvent in processKafkaEvents) {
                coordinatorEventListeners.forEach { it.onCancelOrStopProcess(data.deletedEventId) }
                eventManager.deleteProcessEvent(kafkaPayload.value.referenceId, data.deletedEventId)
            }
        } else {
            log.warn { "Deletion handling was triggered with wrong data" }
        }
    }

    fun readAllAvailableInQueue() {
        val messages = eventManager.getProcessEventsClaimable()
        io.launch {
            messages.forEach {
                delay(1000)
                createTasksBasedOnEventsAndPersistence(referenceId = it.referenceId, eventId = it.eventId, messages)
            }
        }
    }

    fun readAllMessagesFor(referenceId: String, eventId: String) {
        val messages = eventManager.getProcessEventsClaimable()
        createTasksBasedOnEventsAndPersistence(referenceId, eventId, messages)
    }

    private final val processKafkaEvents = listOf(
        KafkaEvents.EventWorkEncodeCreated,
        KafkaEvents.EventWorkExtractCreated,
    )

    private final val acceptEvents = listOf(
        KafkaEvents.EventNotificationOfWorkItemRemoval
    ) + processKafkaEvents


    @Scheduled(fixedDelay = (5*6_0000))
    fun checkForWork() {
        log.info { "Checking if there is any work to do.." }
        readAllAvailableInQueue()
    }

    interface CoordinatorEvents {
        fun onCancelOrStopProcess(eventId: String)
    }

}