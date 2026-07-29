package no.iktdev.mediaprocessing.processer.services

import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.shared.common.sse.SSEServerImplementation
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SSEServer(
    private val progressCache: LocalProgressCache
): SSEServerImplementation() {

    fun notify(emitter: SseEmitter) {
        emitter.send()
    }
}