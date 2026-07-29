package no.iktdev.mediaprocessing.shared.common.sse

import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CopyOnWriteArrayList

abstract class SSEServerImplementation {
    private val emitters = CopyOnWriteArrayList<SseEmitter>()



    fun createEmitter(): SseEmitter {
        val emitter = SseEmitter(0L) // never timeout
        emitters.add(emitter)

        emitter.onCompletion { emitters.remove(emitter) }
        emitter.onTimeout { emitters.remove(emitter) }
        emitter.onError { emitters.remove(emitter) }

        return emitter
    }

    fun broadcast(eventName: String, data: Any) {
        val dead = mutableListOf<SseEmitter>()

        emitters.forEach { emitter ->
            try {
                emitter.send(
                    SseEmitter.event()
                        .name(eventName)
                        .data(data)
                )
            } catch (ex: Exception) {
                // Debug: klienten er borte
                println("SSE client disconnected: ${ex.message}")
                dead.add(emitter)
            }
        }

        if (dead.isNotEmpty()) {
            emitters.removeAll(dead.toSet())
        }
    }

}
