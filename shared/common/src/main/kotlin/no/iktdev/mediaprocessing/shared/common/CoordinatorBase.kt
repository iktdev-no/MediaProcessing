package no.iktdev.mediaprocessing.shared.common

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.CoroutinesDefault
import no.iktdev.mediaprocessing.shared.common.tasks.EventBasedMessageListener
import no.iktdev.mediaprocessing.shared.common.tasks.TaskCreatorImpl
import no.iktdev.mediaprocessing.shared.kafka.core.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.DeserializedConsumerRecord
import no.iktdev.mediaprocessing.shared.kafka.dto.Message
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import org.springframework.context.ApplicationContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

abstract class CoordinatorBase<V, L: EventBasedMessageListener<V>> {
    val defaultCoroutine = CoroutinesDefault()
    private var ready: Boolean = false
    fun isReady() = ready
    private val log = KotlinLogging.logger {}
    abstract val listeners: L

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    lateinit var producer: CoordinatorProducer

    @Autowired
    private lateinit var listener: DefaultMessageListener

    abstract fun createTasksBasedOnEventsAndPersistence(referenceId: String, eventId: String, messages: List<V>)

    open fun onCoordinatorReady() {
        log.info { "Attaching listeners to Coordinator" }
        listener.onMessageReceived = { event -> onMessageReceived(event)}
        listener.listen(KafkaEnv.kafkaTopic)
        ready = true
    }
    abstract fun onMessageReceived(event: DeserializedConsumerRecord<KafkaEvents, Message<out MessageDataWrapper>>)

    fun isAllServicesRegistered(): Boolean {
        val services = context.getBeansWithAnnotation(Service::class.java).values.map { it.javaClass }.filter { TaskCreatorImpl.isInstanceOfTaskCreatorImpl(it) }
        val loadedServices = listeners.listeners.map { it.taskHandler.javaClass as Class<Any> }
        val notPresent = services.filter { it !in loadedServices }

        notPresent.forEach {
            log.warn { "Waiting for ${it.simpleName} to attach.." }
        }

        return notPresent.isEmpty()
    }

    @PostConstruct
    fun onInitializationCompleted() {
        defaultCoroutine.launch {
            while (!isAllServicesRegistered()) {
                log.info { "Waiting for mandatory services to start" }
                delay(1000)
            }
        }.invokeOnCompletion {
            onCoordinatorReady()
            log.info { "Coordinator is Ready!" }
        }
    }
}