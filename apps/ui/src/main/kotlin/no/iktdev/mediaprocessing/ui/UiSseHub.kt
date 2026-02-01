package no.iktdev.mediaprocessing.ui

import jakarta.annotation.PostConstruct
import no.iktdev.mediaprocessing.ui.dto.SSEMessage
import no.iktdev.mediaprocessing.ui.service.CoordinatorClient
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

@Component
class UiSseHub(
    private val coordinator: CoordinatorClient,
) {
    private val emitters = CopyOnWriteArrayList<SseEmitter>()
    private var listeners: MutableList<SSEStateListener> = mutableListOf()
    private var currentState: CurrentState = CurrentState.DISCONNECTED

    init {
        log.info { "UiSseHub initialized" }
    }

    @PostConstruct
    fun startSSE() {
        log.info { "Starting connection to SSE" }
        coordinator.connectToSse(onConnected = {
            currentState = CurrentState.CONNECTED
            log.info { "Connected to SSE" }
            listeners.onEach { l -> l.onConnected() } }, onReconnecting = {
            currentState = CurrentState.RECONNECTING
            listeners.onEach { l -> l.onReconnecting() }
        }, onDisconnected = {
            currentState = CurrentState.DISCONNECTED
            log.warn { "Lost connection to SSE" }
            listeners.onEach { l -> l.onDisconnected() }
        }).subscribe { event ->
            val eventName = event.event()
            if (eventName == null) {
                log.error("Received SSE event with no event name ${event.data()}")
            } else {
                broadcast(event, eventName)
            }
        }
    }

    fun registerListener(listener: SSEStateListener) {
        listeners.add(listener)
        when (currentState) {
            CurrentState.CONNECTED -> listener.onConnected()
            CurrentState.RECONNECTING -> listener.onReconnecting()
            CurrentState.DISCONNECTED -> listener.onDisconnected()
        }
    }

    fun createEmitter(): SseEmitter {
        val emitter = SseEmitter(0L)
        emitters.add(emitter)

        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }

        return emitter
    }

    fun broadcast(event: Any, name: String) {
        val dead = mutableListOf<SseEmitter>()
        emitters.forEach { emitter ->
            try {
                val data = SSEMessage(name, event)
                val builder = SseEmitter.event().data(data)
                emitter.send(builder)
            } catch (ex: Exception) {
                dead.add(emitter)
            }
        }
        emitters.removeAll(dead)
    }

    enum class CurrentState {
        CONNECTED,
        RECONNECTING,
        DISCONNECTED
    }

    interface SSEStateListener {
        fun onConnected()
        fun onReconnecting()
        fun onDisconnected()
    }
}