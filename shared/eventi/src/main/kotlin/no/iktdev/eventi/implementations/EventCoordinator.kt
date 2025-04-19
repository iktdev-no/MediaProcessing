package no.iktdev.eventi.implementations

import kotlinx.coroutines.*
import mu.KotlinLogging
import no.iktdev.eventi.EventDeadlockDetector
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
    private var coroutine = CoroutineScope(Dispatchers.IO + SupervisorJob())

    open var ready: Boolean = false
    fun isReady(): Boolean {
        return ready
    }

    open fun onReady() {
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


    private var wasActiveNotify: Boolean = true
    private fun onEventCollectionReceived(referenceId: String, events: List<T>) {
        val orphanedReferences = referencePool.filter { !it.value.isActive }.filter { id -> id.key !in referenceId }.map { it.key }
        orphanedReferences.forEach { id -> referencePool.remove(id) }

        activePolls = referencePool.values.filter { it.isActive }.size

        val isAvailable = if (referenceId in referencePool.keys) {
            referencePool[referenceId]?.isActive != true
        } else true

        if (isAvailable) {
            referencePool[referenceId] = coroutine.async {
                onEventsReceived(events)
            }
        }

    }

    val bashingReferenceObject: MutableMap<String, MutableList<Pair<String, Long>>> = mutableMapOf()

    private suspend fun onEventsReceived(events: List<T>): Boolean = coroutineScope {
        val listeners = getListeners()
        events.forEach { event ->
            listeners.forEach { listener ->
                if (listener.shouldIProcessAndHandleEvent(event, events)) {
                    val consumableEvent = ConsumableEvent(event)
                    listener.onEventsReceived(consumableEvent, events)
                    if (consumableEvent.isConsumed) {
                        val referenceId = events.first().referenceId()
                        val listenerName = listener::class.java.simpleName
                        val eventId = consumableEvent.metadata().eventId
                        val bashingId = "$eventId-$listenerName"
                        bashingReferenceObject.computeIfAbsent(referenceId) { mutableListOf() }.add(Pair(bashingId, System.currentTimeMillis()))

                        // 🚨 Suppress logging hvis det er en deadlock
                        if (EventDeadlockDetector.detect(referenceId, listenerName, event.eventType.toString())) {
                            log.info { "Consumption detected for $referenceId -> $listenerName on event ${event.eventType}" }
                            EventDeadlockDetector.resolve(referenceId, listenerName, event.eventType.toString())
                        }

                        val bashed = bashingReferenceObject[referenceId]?.takeLast(10) ?: emptyList()
                        if (bashed.size == 10 && bashed.all { it == bashed.first() }) {
                            // We have entered a deadlock here
                            // Due to the nature of the deadlock the event will be determined to be dead
                            log.error { "Producing Failure on $referenceId on event ${event.eventType} due to deadlock in $listenerName" }
                            listener.produceFailure(event)
                        }

                        return@coroutineScope true
                    }
                }
            }
        }
        val threshold = System.currentTimeMillis() - 300_000
        bashingReferenceObject.entries.removeIf { entry ->
            entry.value.removeIf { it.second < threshold }
            entry.value.isEmpty() // Fjern oppføringen hvis listen nå er tom
        }
        log.debug { "No consumption detected for ${events.first().referenceId()}" }
        false
    }

    private var newEventsProducedOnReferenceId: AtomicReference<List<String>> = AtomicReference(emptyList())
    var cachedReferenceList: MutableList<String> = mutableListOf()
    private fun pullForEvents() {
        coroutine.launch {
            while (taskMode == ActiveMode.Active && coroutine.isActive) {
                if (referencePoolIsReadyForEvents()) {
                    log.debug { "New pull on database" }
                    val referenceIdsAvailable = eventManager.getAvailableReferenceIds()

                    val newReferenceIds = referenceIdsAvailable.subtract(cachedReferenceList.toSet())
                    cachedReferenceList = referenceIdsAvailable.toMutableList()

                    if (newReferenceIds.isNotEmpty()) {
                        log.info { "New referenceIds found:\n\t -> ${newReferenceIds.joinToString("\n\t -> ")}" }
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
            log.warn { "Do not produce is enabled!" }
            newEventProduced = true
            return true
        }

        val isStored = eventManager.storeEvent(event)
        if (isStored) {
            log.debug { "Stored event: ${event.eventType}" }
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