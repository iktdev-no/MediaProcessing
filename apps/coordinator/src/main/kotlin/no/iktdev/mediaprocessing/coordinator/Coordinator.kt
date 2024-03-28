package no.iktdev.mediaprocessing.coordinator

import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.coordinator.coordination.PersistentEventBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.CoordinatorBase
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.dto.ProcessStartOperationEvents
import no.iktdev.mediaprocessing.shared.contract.dto.RequestStartOperationEvents
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.*
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID

@Service
class Coordinator() : CoordinatorBase<PersistentMessage, PersistentEventBasedMessageListener>() {
    val io = Coroutines.io()

    override fun onCoordinatorReady() {
        readAllUncompletedMessagesInQueue()
    }

    override fun onMessageReceived(event: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) {
        val success = persistentWriter.storeEventDataMessage(event.key.event, event.value)
        if (!success) {
            log.error { "Unable to store message: ${event.key.event} in database ${getEventsDatabase().config.databaseName}" }
        } else {
            io.launch {
                delay(500) // Give the database a few sec to update
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
        val operations: List<ProcessStartOperationEvents> = listOf(
            ProcessStartOperationEvents.ENCODE,
            ProcessStartOperationEvents.EXTRACT,
            ProcessStartOperationEvents.CONVERT
        )
        startProcess(file, type, operations)
    }

    fun startProcess(file: File, type: ProcessType, operations: List<ProcessStartOperationEvents>) {
        val processStartEvent = MediaProcessStarted(
            status = Status.COMPLETED,
            file = file.absolutePath,
            type = type
        )
        producer.sendMessage(UUID.randomUUID().toString(), KafkaEvents.EVENT_MEDIA_PROCESS_STARTED, processStartEvent)

    }

    public fun startRequestProcess(file: File, operations: List<RequestStartOperationEvents>): UUID {
        val referenceId: UUID = UUID.randomUUID()
        val start = RequestProcessStarted(
            status = Status.COMPLETED,
            file = file.absolutePath,
            operations = operations
        )
        producer.sendMessage(referenceId = referenceId.toString(), KafkaEvents.EVENT_REQUEST_PROCESS_STARTED, start)
        return referenceId
    }

    fun permitWorkToProceedOn(referenceId: String, message: String) {
        producer.sendMessage(referenceId = referenceId, KafkaEvents.EVENT_MEDIA_WORK_PROCEED_PERMITTED, SimpleMessageData(Status.COMPLETED, message))
    }


    fun readAllUncompletedMessagesInQueue() {
        val messages = persistentReader.getUncompletedMessages()
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
        val messages = persistentReader.getMessagesFor(referenceId)
        if (messages.find { it.eventId == eventId && it.referenceId == referenceId } == null) {
            log.warn { "EventId ($eventId) for ReferenceId ($referenceId) has not been made available in the database yet." }
            io.launch {
                val fixedDelay = 1000L
                delay(fixedDelay)
                var delayed = 0L
                var msc = persistentReader.getMessagesFor(referenceId)
                while (msc.find { it.eventId == eventId } != null || delayed < 1000 * 60) {
                    delayed += fixedDelay
                    msc = persistentReader.getMessagesFor(referenceId)
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

        io.launch {
            buildModelBasedOnMessagesFor(referenceId, messages)
        }
    }

    fun getProcessStarted(messages: List<PersistentMessage>): MediaProcessStarted? {
        return messages.find { it.event == KafkaEvents.EVENT_MEDIA_PROCESS_STARTED }?.data as MediaProcessStarted
    }

    suspend fun buildModelBasedOnMessagesFor(referenceId: String, messages: List<PersistentMessage>) {
        if (messages.any { it.data is ProcessCompleted }) {
            // TODO: Build and insert into database
        }
    }
}


