package no.iktdev.eventi.implementations

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.eventi.data.EventImpl
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service

abstract class EventCoordinator<T: EventImpl, E: EventsManagerImpl<T>> {
    abstract var applicationContext: ApplicationContext
    abstract var eventManager: E


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


    private fun onEventsReceived(list: List<T>) = runBlocking {
        val listeners = getListeners()
        list.groupBy { it.metadata.referenceId }.forEach { (referenceId, events) ->
            launch {
                events.forEach { event ->
                    listeners.forEach { listener ->
                        if (listener.shouldIProcessAndHandleEvent(event, events))
                            listener.onEventsReceived(event, events)
                    }
                }
            }
        }
    }


    private var newItemReceived: Boolean = false
    private fun pullForEvents() {
        coroutine.launch {
            while (taskMode == ActiveMode.Active) {
                val events = eventManager?.readAvailableEvents()
                if (events == null) {
                    log.warn { "EventManager is not loaded!" }
                } else {
                    onEventsReceived(events)
                }
                waitForConditionOrTimeout(5000) { newItemReceived }.also {
                    newItemReceived = false
                }
            }
        }
    }


    fun getListeners(): List<EventListenerImpl<T, *>> {
        val serviceBeans: Map<String, Any> = applicationContext.getBeansWithAnnotation(Service::class.java)

        val beans =  serviceBeans.values.stream()
            .filter { bean: Any? -> bean is EventListenerImpl<*, *> }
            .map { it -> it as EventListenerImpl<*, *> }
            .toList()
        return beans as List<EventListenerImpl<T, *>>
    }


    /**
     * @return true if its stored
     */
    fun produceNewEvent(event: T): Boolean {
        val isStored =  eventManager?.storeEvent(event) ?: false
        if (isStored) {
            newItemReceived = true
        }
        return isStored
    }

    suspend fun waitForConditionOrTimeout(timeout: Long, condition: () -> Boolean) {
        val startTime = System.currentTimeMillis()

        withTimeout(timeout) {
            while (!condition()) {
                delay(100)
                if (System.currentTimeMillis() - startTime >= timeout) {
                    break
                }
            }
        }
    }
}

// TODO: Ikke implementert enda
enum class ActiveMode {
    Active,
    Passive
}