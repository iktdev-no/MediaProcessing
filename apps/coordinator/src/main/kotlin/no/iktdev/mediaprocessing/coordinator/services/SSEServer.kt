package no.iktdev.mediaprocessing.coordinator.services

import no.iktdev.mediaprocessing.shared.common.sse.SSEServerImplementation
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SSEServer(
): SSEServerImplementation() {

    fun notify(emitter: SseEmitter) {
        TODO("Not implemented yet")
    }
}