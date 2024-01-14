package no.iktdev.mediaprocessing.converter

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.converter.flow.EventBasedProcessMessageListener
import no.iktdev.mediaprocessing.shared.common.CoordinatorBase
import no.iktdev.mediaprocessing.shared.common.DatabaseConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.stereotype.Service

@Service
class ConverterCoordinator() : CoordinatorBase<PersistentProcessDataMessage, EventBasedProcessMessageListener>() {
    val io = Coroutines.io()

    private val log = KotlinLogging.logger {}

    override val listeners: EventBasedProcessMessageListener = EventBasedProcessMessageListener()
    override fun createTasksBasedOnEventsAndPersistence(
        referenceId: String,
        eventId: String,
        messages: List<PersistentProcessDataMessage>
    ) {
        val triggeredMessage = messages.find { it.eventId == eventId }
        if (triggeredMessage == null) {
            log.error { "Could not find $eventId in provided messages" }
            return
        }
        listeners.forwardEventMessageToListeners(triggeredMessage, messages)
    }

    fun readAllMessagesFor(referenceId: String, eventId: String) {
        val messages = PersistentDataReader().getAvailableProcessEvents()
        createTasksBasedOnEventsAndPersistence(referenceId, eventId, messages)
    }

    fun readAllInQueue() {
        val messages = PersistentDataReader().getAvailableProcessEvents()
        io.launch {
            messages.forEach {
                delay(1000)
                createTasksBasedOnEventsAndPersistence(referenceId = it.referenceId, eventId = it.eventId, messages)
            }
        }
    }

    override fun onCoordinatorReady() {
        log.info { "Converter Coordinator is ready" }
        readAllInQueue()
    }

    override fun onMessageReceived(event: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) {
        if (event.key == KafkaEvents.EVENT_WORK_CONVERT_CREATED) {
            val success = PersistentDataStore().storeProcessDataMessage(event.key.event, event.value)
            if (!success) {
                log.error { "Unable to store message: ${event.key.event} in database ${DatabaseConfig.database}!" }
            } else {
                readAllMessagesFor(event.value.referenceId, event.value.eventId)
            }
        } else if (event.key == KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED) {
            readAllInQueue()
        } else {
            log.debug { "Skipping ${event.key}" }
        }
        //log.info { Gson().toJson(event.value) }

    }
}