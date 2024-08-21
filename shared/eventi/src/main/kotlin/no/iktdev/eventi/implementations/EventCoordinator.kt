package no.iktdev.eventi.implementations

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.data.EventImpl
import no.iktdev.eventi.data.referenceId
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

abstract class EventCoordinator<T : EventImpl, E : EventsManagerImpl<T>> {
    abstract var applicationContext: ApplicationContext
    abstract var eventManager: E

    val pullDelay: AtomicLong = AtomicLong(1000)
    val fastPullDelay: AtomicLong = AtomicLong(500)
    val slowPullDelay: AtomicLong = AtomicLong(2500)

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


    open var taskMode: ActiveMode = ActiveMode.Active
    private val referencePool: MutableMap<String, Deferred<Boolean>> = mutableMapOf()
    private fun referencePoolIsReadyForEvents(): Boolean {
        return (referencePool.isEmpty() || referencePool.any { !it.value.isActive })
    }

    private var newEventProduced: Boolean = false

    abstract fun getActiveTaskMode(): ActiveMode

    private var activePolls: Int = 0
    data class PollStats(val active: Int, val total: Int)
    fun getActivePolls(): PollStats {
        return PollStats(active = activePolls, total = referencePool.values.size)
    }

    private fun onEventGroupsReceived(eventGroup: List<List<T>>) {
        val egRefIds = eventGroup.map { it.first().referenceId() }

        val orphanedReferences = referencePool.filter { !it.value.isActive }.filter { id -> id.key !in egRefIds }.map { it.key }
        orphanedReferences.forEach { id -> referencePool.remove(id) }

        activePolls = referencePool.values.filter { it.isActive }.size
        if (orphanedReferences.isNotEmpty() && referencePool.isEmpty()) {
            log.info { "Last active references removed from pull pool, " }
        }

        if (eventGroup.isEmpty()) {
            return
        }

        eventGroup.forEach {
            val referenceId = it.first().referenceId()

            val isAvailable = if (referenceId in referencePool.keys) {
                referencePool[referenceId]?.isActive != true
            } else true

            if (isAvailable) {
                referencePool[referenceId] = coroutine.async {
                    onEventsReceived(it)
                }
            }
        }
    }

    private var wasActiveNotify: Boolean = true
    private fun onEventCollectionReceived(referenceId: String, events: List<T>) {
        val orphanedReferences = referencePool.filter { !it.value.isActive }.filter { id -> id.key !in referenceId }.map { it.key }
        orphanedReferences.forEach { id -> referencePool.remove(id) }

        activePolls = referencePool.values.filter { it.isActive }.size
        if (orphanedReferences.isNotEmpty() && referencePool.isEmpty() && wasActiveNotify) {
            log.info { "Last active references removed from pull pool, $referenceId" }
            wasActiveNotify = false
        } else {
            wasActiveNotify = true
        }

        val isAvailable = if (referenceId in referencePool.keys) {
            referencePool[referenceId]?.isActive != true
        } else true

        if (isAvailable) {
            referencePool[referenceId] = coroutine.async {
                onEventsReceived(events)
            }
        }

    }


    private suspend fun onEventsReceived(events: List<T>): Boolean = coroutineScope {
        val listeners = getListeners()
        events.forEach { event ->
            listeners.forEach { listener ->
                if (listener.shouldIProcessAndHandleEvent(event, events)) {
                    val consumableEvent = ConsumableEvent(event)
                    listener.onEventsReceived(consumableEvent, events)
                    if (consumableEvent.isConsumed) {
                        log.info { "Consumption detected for ${events.first().referenceId()} -> ${listener::class.java.simpleName} on event ${event.eventType}" }
                        return@coroutineScope true
                    }
                }
            }
        }
        log.debug { "No consumption detected for ${events.first().referenceId()}" }
        false
    }

    private var newEventsProducedOnReferenceId: AtomicReference<List<String>> = AtomicReference(emptyList())
    var cachedReferenceList: MutableList<String> = mutableListOf()
    private fun pullForEvents() {
        coroutine.launch {
            while (taskMode == ActiveMode.Active) {
                if (referencePoolIsReadyForEvents()) {
                    log.debug { "New pull on database" }
                    val referenceIdsAvailable = eventManager.getAvailableReferenceIds()

                    val newReferenceIds = cachedReferenceList.subtract(referenceIdsAvailable.toSet())
                    cachedReferenceList = referenceIdsAvailable.toMutableList()

                    if (newReferenceIds.isNotEmpty()) {
                        log.info { "New referenceIds found,\n ${newReferenceIds.joinToString("\n")}" }
                    }

                    for (referenceId in referenceIdsAvailable) {
                        val events = eventManager.readAvailableEventsFor(referenceId)
                        onEventCollectionReceived(referenceId, events)
                    }

                    if (referenceIdsAvailable.isNotEmpty()) {
                        if (pullDelay.get() != fastPullDelay.get()) {
                            log.info { "Available events found, switching to fast pull @ Delay -> ${fastPullDelay.get()}" }
                        }
                        pullDelay.set(fastPullDelay.get())
                    } else {
                        if (pullDelay.get() != slowPullDelay.get()) {
                            log.info { "No events available, switching to slow pull @ Delay -> ${slowPullDelay.get()}" }
                        }
                        pullDelay.set(slowPullDelay.get())
                    }
                }
                waitForConditionOrTimeout(pullDelay.get()) {
                    newEventProduced
                }
                newEventProduced = false
            }
            taskMode = getActiveTaskMode()
        }
    }

    private var cachedListeners: List<String> = emptyList()
    @SuppressWarnings("unchecked cast")
    fun getListeners(): List<EventListenerImpl<T, *>> {
        val serviceBeans: Map<String, Any> = applicationContext.getBeansWithAnnotation(Service::class.java)

        val beans = serviceBeans.values.stream()
            .filter { bean: Any? -> bean is EventListenerImpl<*, *> }
            .map { it -> it as EventListenerImpl<*, *> }
            .toList()
        val eventListeners: List<EventListenerImpl<T, *>> = beans as List<EventListenerImpl<T, *>>
        val listenerNames = eventListeners.map { it::class.java.name }
        if (listenerNames != cachedListeners) {
            listenerNames.filter { it !in cachedListeners }.forEach {
                log.info { "Registered new listener $it" }
            }
        }
        cachedListeners = listenerNames
        return eventListeners
    }

    var doNotProduce = System.getenv("DISABLE_PRODUCE").toBoolean() ?: false
    /**
     * @return true if its stored
     */
    fun produceNewEvent(event: T): Boolean {
        if (doNotProduce) {
            newEventProduced = true
            return true
        }

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

enum class ActiveMode {
    Active,
    Passive
}