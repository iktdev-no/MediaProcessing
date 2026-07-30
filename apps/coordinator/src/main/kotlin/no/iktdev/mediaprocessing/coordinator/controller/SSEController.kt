package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.services.SSEServer
import no.iktdev.mediaprocessing.shared.common.sse.SSEControllerImplementation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/sse")
class SSEController(private val sse: SSEServer): SSEControllerImplementation(sse) {
    @GetMapping
    override fun stream(): SseEmitter {
        val emitter = super.stream()
        sse.notify(emitter)
        return emitter
    }
}