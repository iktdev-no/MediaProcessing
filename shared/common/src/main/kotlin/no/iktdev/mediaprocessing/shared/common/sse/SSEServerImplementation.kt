package no.iktdev.mediaprocessing.shared.common.sse

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.sse.basemodel.SSEPingEvent
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class SSEServerImplementation(val isBackend: Boolean = false) {
    private val emitters = CopyOnWriteArrayList<SseEmitter>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val log = KotlinLogging.logger {}


    init {
        // Send en ping-hendelse hvert 10. sekund for å holde forbindelsen i live
        scheduler.scheduleAtFixedRate({
            broadcast(SSEPingEvent(System.currentTimeMillis()))
        }, 0, 5, TimeUnit.SECONDS)
    }

    fun createEmitter(): SseEmitter {
        val emitter = SseEmitter(0L) // never timeout
        emitters.add(emitter)

        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
        emitter.onError { emitters.remove(emitter) }

        return emitter
    }

    fun broadcast(event: SSEEvent) {
        val dead = mutableListOf<SseEmitter>()
        emitters.forEach { emitter ->
            try {
                if (isBackend) {
                    emitter.send(SseEmitter.event().name(event.type)
                        .data(event))
                } else {
                    emitter.send(
                        SseEmitter.event()
                            .data(event)
                    )
                }
            } catch (ex: Exception) {
                println("SSE client disconnected: ${ex.message}")
                dead.add(emitter)
            }
        }

        if (dead.isNotEmpty()) {
            emitters.removeAll(dead.toSet())
        }
    }
}