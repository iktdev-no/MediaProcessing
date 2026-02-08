package no.iktdev.mediaprocessing.ui

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import no.iktdev.mediaprocessing.ui.dto.SSEMessage
import no.iktdev.mediaprocessing.ui.service.CoordinatorClient
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import reactor.core.Disposable
import java.util.concurrent.CopyOnWriteArrayList

@Component
class UiSseHub(
    private val coordinator: CoordinatorClient,
    private val objectMapper: ObjectMapper
) {
    private val emitters = CopyOnWriteArrayList<SseEmitter>()
    private var listeners: MutableList<SSEStateListener> = mutableListOf()
    private var currentState: CurrentState = CurrentState.DISCONNECTED

    private var sseSubscription: Disposable? = null

    init {
        log.info { "UiSseHub initialized" }
    }

    @PostConstruct
    fun startSSE() {
        log.info { "Starting connection to SSE" }
        sseSubscription = coordinator.connectToSse(onConnected = {
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
            val raw = event.data()

            if (eventName == null || raw == null) {
                log.error { "Received unknown event $eventName" }
                return@subscribe
            }

            val parsed = try {
                objectMapper.readValue(raw as String, Any::class.java)
            } catch (ex: Exception) {
                log.error("Failed to parse SSE Payload $raw", ex)
            }
            broadcast(parsed, eventName)
        }
    }

    @PreDestroy
    fun shutdown() {
        log.info { "Shutting down SSE connection" }
        sseSubscription?.dispose()
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
        val msg = SSEMessage(name, event)
        val data = SseEmitter.event()
            .data(msg)
        emitters.forEach { emitter ->
            try {
                emitter.send(data)
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