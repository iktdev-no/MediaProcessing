package no.iktdev.mediaprocessing.converter

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.converter.coordination.PersistentEventProcessBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.CoordinatorBase
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.common.persistance.events
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@EnableScheduling
@Service
class ConverterCoordinator() : CoordinatorBase<PersistentProcessDataMessage, PersistentEventProcessBasedMessageListener>() {
    val io = Coroutines.io()

    private val log = KotlinLogging.logger {}

    override val listeners = PersistentEventProcessBasedMessageListener()
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

    override fun onCoordinatorReady() {
        super.onCoordinatorReady()
        log.info { "Converter Coordinator is ready" }
        generateMissingEvents()
        readAllInQueue()
    }


    override fun onMessageReceived(event: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>) {
        if (event.key == KafkaEvents.EventWorkConvertCreated) {
            val success = eventManager.setProcessEvent(event.key, event.value)
            if (!success) {
                log.error { "Unable to store message event: ${event.key.event} with eventId ${event.value.eventId} with referenceId ${event.value.referenceId} in database ${getEventsDatabase().database}!" }
            } else {
                io.launch {
                    delay(500)
                    readAllMessagesFor(event.value.referenceId, event.value.eventId)
                }
            }
        } else if (event.key == KafkaEvents.EventWorkExtractPerformed) {
            readAllInQueue()
        } else {
            log.debug { "Skipping ${event.key}" }
        }
    }

    fun readAllInQueue() {
        val messages = eventManager.getProcessEventsClaimable()// persistentReader.getAvailableProcessEvents()
        io.launch {
            messages.forEach {
                delay(1000)
                createTasksBasedOnEventsAndPersistence(referenceId = it.referenceId, eventId = it.eventId, messages)
            }
        }
    }

    private fun generateMissingEvents() {
        val existing = eventManager.getAllProcessEvents().filter { it.event == KafkaEvents.EventWorkConvertCreated }.map { it.eventId }
        val messages = eventManager.getEventsUncompleted()

        val myEvents = messages.flatten()
            .filter { it.event == KafkaEvents.EventWorkConvertCreated }
            .filter { existing.none { en -> en == it.eventId } }

        myEvents.forEach {
            eventManager.setProcessEvent(it.event, Message(
                referenceId = it.referenceId,
                eventId = it.eventId,
                data = it.data
            ))
        }
    }


    fun readAllMessagesFor(referenceId: String, eventId: String) {
        val messages = eventManager.getProcessEventsClaimable() // persistentReader.getAvailableProcessEvents()
        createTasksBasedOnEventsAndPersistence(referenceId, eventId, messages)
    }

    @Scheduled(fixedDelay = (5*6_0000))
    fun checkForWork() {
        log.info { "Checking if there is any work to do.." }
        readAllInQueue()
        generateMissingEvents()
    }

}