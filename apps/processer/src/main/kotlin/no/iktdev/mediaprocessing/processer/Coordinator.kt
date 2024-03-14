package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.processer.coordination.PersistentEventProcessBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.CoordinatorBase
import no.iktdev.mediaprocessing.shared.common.DatabaseConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@EnableScheduling
class Coordinator(): CoordinatorBase<PersistentProcessDataMessage, PersistentEventProcessBasedMessageListener>() {
    private val log = KotlinLogging.logger {}
    val io = Coroutines.io()
    override val listeners = PersistentEventProcessBasedMessageListener()

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
        if (!processKafkaEvents.contains(event.key)) {
            return
        }

        val success = PersistentDataStore().storeProcessDataMessage(event.key.event, event.value)
        if (!success) {
            log.error { "Unable to store message: ${event.key.event} in database ${DatabaseConfig.database}" }
        } else {
            io.launch {
                delay(500)
                readAllMessagesFor(event.value.referenceId, event.value.eventId)
            }
        }
    }




    fun readAllAvailableInQueue() {
        val messages = PersistentDataReader().getAvailableProcessEvents()
        io.launch {
            messages.forEach {
                delay(1000)
                createTasksBasedOnEventsAndPersistence(referenceId = it.referenceId, eventId = it.eventId, messages)
            }
        }
    }

    fun readAllMessagesFor(referenceId: String, eventId: String) {
        val messages = PersistentDataReader().getAvailableProcessEvents()
        createTasksBasedOnEventsAndPersistence(referenceId, eventId, messages)
    }

    val processKafkaEvents = listOf(
        KafkaEvents.EVENT_WORK_ENCODE_CREATED,
        KafkaEvents.EVENT_WORK_EXTRACT_CREATED,
    )


}