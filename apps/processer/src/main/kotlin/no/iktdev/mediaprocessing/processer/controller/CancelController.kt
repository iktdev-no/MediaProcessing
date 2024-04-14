package no.iktdev.mediaprocessing.processer.controller

import no.iktdev.mediaprocessing.processer.Coordinator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class CancelController(@Autowired var coordinator: Coordinator) {

    @RequestMapping(path = ["/cancel"])
    fun cancelProcess(@RequestBody eventId: String? = null): ResponseEntity<String> {
        if (eventId.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No eventId provided!")
        }
        coordinator.getRegisteredEventListeners().forEach { it.onCancelOrStopProcess(eventId) }
        return ResponseEntity.ok(null)
    }

}