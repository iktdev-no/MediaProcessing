package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.mediaprocessing.shared.common.sse.SSEKeys
import no.iktdev.mediaprocessing.shared.common.sse.SSEServerImplementation
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SSEServer(
    private val progressCache: LocalProgressCache
): SSEServerImplementation() {

    fun notify(emitter: SseEmitter) {
        getInitStates().forEach { (key, value) ->
            val event = SseEmitter.event()
                .name(key.key)
                .data(value)
            emitter.send(event)
        }
    }

    fun getInitStates(): List<Pair<SSEKeys, Any>> = listOf(
            SSEKeys.ProgressRestore to progressCache.getAll().values
        )
}