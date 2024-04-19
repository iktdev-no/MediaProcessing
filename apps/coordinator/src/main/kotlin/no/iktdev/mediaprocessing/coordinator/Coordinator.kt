package no.iktdev.mediaprocessing.coordinator

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.coordinator.coordination.PersistentEventBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.CoordinatorBase
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.*
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID

@EnableScheduling
@Service
class Coordinator() : CoordinatorBase<PersistentMessage, PersistentEventBasedMessageListener>() {
    val io = Coroutines.io()

    override fun onCoordinatorReady() {
        super.onCoordinatorReady()
        readAllUncompletedMessagesInQueue()
    }

    override fun onMessageReceived(event: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) {
        val success = eventManager.setEvent(event.key, event.value)
        if (!success) {
            log.error { "Unable to store message event: ${event.key.event} with eventId ${event.value.eventId} with referenceId ${event.value.referenceId} in database ${getEventsDatabase().database}!" }
        } else {
            io.launch {
                delay(1000) // Give the database a few sec to update
                readAllMessagesFor(event.value.referenceId, event.value.eventId)
            }
        }
    }

    override fun createTasksBasedOnEventsAndPersistence(
        referenceId: String,
        eventId: String,
        messages: List<PersistentMessage>
    ) {
        val triggered = messages.find { it.eventId == eventId }
        if (triggered == null) {
            log.error { "Could not find $eventId in provided messages" }
            return
        }
        listeners.forwardEventMessageToListeners(triggered, messages)
    }

    private val log = KotlinLogging.logger {}

    override val listeners = PersistentEventBasedMessageListener()

    //private val forwarder = Forwarder()

    public fun startProcess(file: File, type: ProcessType) {
        val operations: List<StartOperationEvents> = listOf(
            StartOperationEvents.ENCODE,
            StartOperationEvents.EXTRACT,
            StartOperationEvents.CONVERT
        )
        startProcess(file, type, operations)
    }

    fun startProcess(file: File, type: ProcessType, operations: List<StartOperationEvents>): UUID {
        val referenceId: UUID = UUID.randomUUID()
        val processStartEvent = MediaProcessStarted(
            status = Status.COMPLETED,
            file = file.absolutePath,
            type = type
        )
        producer.sendMessage(UUID.randomUUID().toString(), KafkaEvents.EventMediaProcessStarted, processStartEvent)
        return referenceId
    }

    fun permitWorkToProceedOn(referenceId: String, message: String) {
        producer.sendMessage(
            referenceId = referenceId,
            KafkaEvents.EventMediaWorkProceedPermitted,
            SimpleMessageData(Status.COMPLETED, message, null)
        )
    }


    fun readAllUncompletedMessagesInQueue() {
        val messages = eventManager.getEventsUncompleted()
        if (messages.isNotEmpty()) {
            log.info { "Found ${messages.size} uncompleted items" }
        }
        messages.onEach {
            it.firstOrNull()?.let {
                log.info { "Found uncompleted: ${it.referenceId}" }
            }
        }
        io.launch {
            messages.forEach {
                delay(1000)
                try {
                    listeners.forwardBatchEventMessagesToListeners(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun readAllMessagesFor(referenceId: String, eventId: String) {
        val messages = eventManager.getEventsWith(referenceId)
        if (messages.find { it.eventId == eventId && it.referenceId == referenceId } == null) {
            log.warn { "EventId ($eventId) for ReferenceId ($referenceId) has not been made available in the database yet." }
            io.launch {
                val fixedDelay = 1000L
                delay(fixedDelay)
                var delayed = 0L
                var msc = eventManager.getEventsWith(referenceId)
                while (msc.find { it.eventId == eventId } != null || delayed < 1000 * 60) {
                    delayed += fixedDelay
                    msc = eventManager.getEventsWith(referenceId)
                }
                operationToRunOnMessages(referenceId, eventId, msc)
            }
        } else {
            operationToRunOnMessages(referenceId, eventId, messages)
        }
    }

    fun operationToRunOnMessages(referenceId: String, eventId: String, messages: List<PersistentMessage>) {
        try {
            createTasksBasedOnEventsAndPersistence(referenceId, eventId, messages)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProcessStarted(messages: List<PersistentMessage>): MediaProcessStarted? {
        return messages.find { it.event == KafkaEvents.EventMediaProcessStarted }?.data as MediaProcessStarted
    }

    @Scheduled(fixedDelay = (5*6_0000))
    fun checkForWork() {
        if (isReady()) {
            log.info { "Checking if there is any uncompleted event sets" }
            readAllUncompletedMessagesInQueue()
        }
    }

}


