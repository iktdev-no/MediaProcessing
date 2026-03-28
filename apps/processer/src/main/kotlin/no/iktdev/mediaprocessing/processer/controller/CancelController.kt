package no.iktdev.mediaprocessing.processer.controller

import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.mediaprocessing.processer.listeners.SubtitleTaskListener
import no.iktdev.mediaprocessing.processer.listeners.LinearVideoTaskListener
import no.iktdev.mediaprocessing.processer.listeners.SegmentedVideoTaskListener
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
    @Autowired
    lateinit var segmentedVideoTaskListener: SegmentedVideoTaskListener

    @RequestMapping("/cancel/single")
    fun cancelTask(@RequestBody taskId: String? = null): ResponseEntity<String> {
        if (taskId.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No eventId provided!")
        }
        var canceled: Boolean = false
        val listener = listOf(segmentedVideoTaskListener, linearVideoTaskListener, subtitleTaskListener)
            .find { it -> it.currentTaskId?.toString() == taskId }

        if (listener != null) {
            listener.currentJob?.cancel()
            canceled = true
        }

        return if (canceled) ResponseEntity.status(HttpStatus.FOUND).body("Canceled") else ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found!")
    }
}