package no.iktdev.eventi.implementations

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.data.EventImpl
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

abstract class EventCoordinator<T : EventImpl, E : EventsManagerImpl<T>> {
    abstract var applicationContext: ApplicationContext
    abstract var eventManager: E

    val pullDelay: AtomicLong = AtomicLong(5000)

    //private val listeners: MutableList<EventListener<T>> = mutableListOf()

    private val log = KotlinLogging.logger {}
    private var coroutine = CoroutineScope(Dispatchers.IO + Job())

    private var ready: Boolean = false
    fun isReady(): Boolean {
        return ready
    }

    init {
        ready = true
        pullForEvents()

    }


    var taskMode: ActiveMode = ActiveMode.Active

    private var newEventProduced: Boolean = false
    private fun onEventsReceived(events: List<T>) = runBlocking {
        val listeners = getListeners()
        launch {
            events.forEach { event ->
                listeners.forEach { listener ->
                    if (listener.shouldIProcessAndHandleEvent(event, events)) {
                        val consumableEvent = ConsumableEvent(event)
                        listener.onEventsReceived(consumableEvent, events)
                        if (consumableEvent.isConsumed) {
                            log.info { "Consumption detected for ${listener::class.java.simpleName} on event ${event.eventType}" }
                            return@launch
                        }
                    }
                }
            }

        }
    }

    private var newEventsProducedOnReferenceId: AtomicReference<List<String>> = AtomicReference(emptyList())
    private fun pullForEvents() {
        coroutine.launch {
            while (taskMode == ActiveMode.Active) {
                val events = eventManager?.readAvailableEvents()
                if (events == null) {
                    log.warn { "EventManager is not loaded!" }
                } else {
                    events.forEach { group ->
                        onEventsReceived(group)
                    }
                }
                waitForConditionOrTimeout(pullDelay.get()) { newEventProduced }.also {
                    newEventProduced = false
                }
            }
        }
    }

    private var cachedListeners: List<String> = emptyList()
    fun getListeners(): List<EventListenerImpl<T, *>> {
        val serviceBeans: Map<String, Any> = applicationContext.getBeansWithAnnotation(Service::class.java)

        val beans = serviceBeans.values.stream()
            .filter { bean: Any? -> bean is EventListenerImpl<*, *> }
            .map { it -> it as EventListenerImpl<*, *> }
            .toList()
        val eventListeners = beans as List<EventListenerImpl<T, *>>
        val listenerNames = eventListeners.map { it::class.java.name }
        if (listenerNames != cachedListeners) {
            listenerNames.filter { it !in cachedListeners }.forEach {
                log.info { "Registered new listener $it" }
            }
        }
        cachedListeners = listenerNames
        return eventListeners
    }


    /**
     * @return true if its stored
     */
    fun produceNewEvent(event: T): Boolean {
        val isStored = eventManager.storeEvent(event)
        if (isStored) {
            log.info { "Stored event: ${event.eventType}" }
            newEventProduced = true
        } else {
            log.error { "Failed to store event: ${event.eventType}" }
        }
        return isStored
    }

    suspend fun waitForConditionOrTimeout(timeout: Long, condition: () -> Boolean) {
        val startTime = System.currentTimeMillis()

        try {
            withTimeout(timeout) {
                while (!condition()) {
                    delay(100)
                    if (System.currentTimeMillis() - startTime >= timeout) {
                        break
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            // Do nothing
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// TODO: Ikke implementert enda
enum class ActiveMode {
    Active,
    Passive
}