package no.iktdev.mediaprocessing.shared.common.sse

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

abstract class SSEControllerImplementation(protected val server: SSEServerImplementation) {
    open fun stream(): SseEmitter {
        return server.createEmitter()
    }
}