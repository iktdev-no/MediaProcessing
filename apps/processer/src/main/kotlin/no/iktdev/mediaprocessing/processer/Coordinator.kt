package no.iktdev.mediaprocessing.processer

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.DatabaseConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataReader
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentDataStore
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
@EnableScheduling
class Coordinator() {

    @Autowired
    private lateinit var producer: CoordinatorProducer

    @Autowired
    private lateinit var listener: DefaultMessageListener

    private val log = KotlinLogging.logger {}

    val listeners = EventBasedMessageListener()

    val io = Coroutines.io()

    fun readAllAvailableInQueue() {
        val messages = PersistentDataReader().getAvailableProcessEvents()
        io.launch {
            messages.forEach {
                delay(1000)
                createTasksBasedOnEventsAndPersistance(referenceId = it.referenceId, eventId = it.eventId, messages)
            }
        }
    }

    fun readAllMessagesFor(referenceId: String, eventId: String) {
        val messages = PersistentDataReader().getAvailableProcessEvents()
        createTasksBasedOnEventsAndPersistance(referenceId, eventId, messages)
    }

    fun createTasksBasedOnEventsAndPersistance(referenceId: String, eventId: String, messages: List<PersistentProcessDataMessage>) {
        val triggered = messages.find { it.eventId == eventId }
        if (triggered == null) {
            log.error { "Could not find $eventId in provided messages" }
            return
        }
        listeners.forwardEventMessageToListeners(triggered, messages)
    }

    val processKafkaEvents = listOf(
        KafkaEvents.EVENT_WORK_ENCODE_CREATED,
        KafkaEvents.EVENT_WORK_EXTRACT_CREATED,
    )

    @PostConstruct
    fun onReady() {
        io.launch {
            listener.onMessageReceived = { event ->
                if (event.key in processKafkaEvents) {
                    val success = PersistentDataStore().storeProcessDataMessage(event.key.event, event.value)
                    if (!success) {
                        log.error { "Unable to store message: ${event.key.event} in database ${DatabaseConfig.database}!" }
                    } else
                        readAllMessagesFor(event.value.referenceId, event.value.eventId)
                } else if (event.key in listOf(KafkaEvents.EVENT_WORK_ENCODE_PERFORMED, KafkaEvents.EVENT_WORK_EXTRACT_PERFORMED, KafkaEvents.EVENT_WORK_EXTRACT_SKIPPED, KafkaEvents.EVENT_WORK_ENCODE_SKIPPED)) {
                    readAllAvailableInQueue()
                } else {
                    log.debug { "Skipping ${event.key}" }
                }
            }
            listener.listen(KafkaEnv.kafkaTopic)
        }
        readAllAvailableInQueue()
    }

}