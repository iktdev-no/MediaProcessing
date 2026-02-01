package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.ProcesserClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/log")
class LogController(
    private val processerClient: ProcesserClient
) {

    @GetMapping
    fun getLog(@RequestParam path: String): Mono<String> {
        return processerClient.fetchLog(path)
    }
}

