package no.iktdev.mediaprocessing.shared.common

import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentProcessDataMessage
import no.iktdev.mediaprocessing.shared.common.tasks.EventBasedMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct

abstract class CoordinatorBase<V, L: EventBasedMessageListener<V>> {
    abstract val listeners: L

    val io = Coroutines.io()

    @Autowired
    lateinit var producer: CoordinatorProducer

    @Autowired
    private lateinit var listener: DefaultMessageListener

    abstract fun createTasksBasedOnEventsAndPersistence(referenceId: String, eventId: String, messages: List<V>)

    abstract fun onCoordinatorReady()
    abstract fun onMessageReceived(event: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>)
    @PostConstruct
    fun onInitializationCompleted() {
        onCoordinatorReady()
        listener.onMessageReceived = { event -> onMessageReceived(event)}
        listener.listen(KafkaEnv.kafkaTopic)
    }
}