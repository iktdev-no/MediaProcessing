package no.iktdev.mediaprocessing.processer.controller

import no.iktdev.mediaprocessing.processer.listeners.SubtitleTaskListener
import no.iktdev.mediaprocessing.processer.listeners.LinearVideoTaskListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class CancelController {
    @Autowired
    lateinit var linearVideoTaskListener: LinearVideoTaskListener
    @Autowired
    lateinit var subtitleTaskListener: SubtitleTaskListener

    @RequestMapping("/cancel/single")
    fun cancelTask(@RequestBody eventId: String? = null): ResponseEntity<String> {
        if (eventId.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No eventId provided!")
        }
        var canceled: Boolean = false
        if (linearVideoTaskListener.currentTaskId?.toString() == eventId) {
            linearVideoTaskListener.currentJob?.cancel()
            canceled = true
        }
        if (subtitleTaskListener.currentTaskId?.toString() == eventId) {
            subtitleTaskListener.currentJob?.cancel()
            canceled = true
        }
        return if (canceled) ResponseEntity.status(HttpStatus.FOUND).body("Canceled") else ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found!")
    }
}