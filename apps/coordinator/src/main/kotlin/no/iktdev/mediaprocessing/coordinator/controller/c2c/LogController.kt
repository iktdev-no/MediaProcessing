package no.iktdev.mediaprocessing.coordinator.controller.c2c

import no.iktdev.mediaprocessing.coordinator.ProcesserClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/processer/log")
class LogController(
    private val processerClient: ProcesserClient
) {

    @GetMapping
    fun getLog(@RequestParam path: String): Mono<ResponseEntity<String>> =
        processerClient.fetchLog(path)

}