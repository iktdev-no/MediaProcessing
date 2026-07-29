package no.iktdev.mediaprocessing.processer.controller

import no.iktdev.mediaprocessing.processer.listeners.SubtitleTaskListener
import no.iktdev.mediaprocessing.processer.listeners.LinearVideoTaskListener
import no.iktdev.mediaprocessing.processer.listeners.SegmentedVideoTaskListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/tasks")
class TasksController {
    @Autowired
    lateinit var linearVideoTaskListener: LinearVideoTaskListener
    @Autowired
    lateinit var subtitleTaskListener: SubtitleTaskListener
    @Autowired
    lateinit var segmentedVideoTaskListener: SegmentedVideoTaskListener

    @PostMapping("/{taskId}/cancel")
    fun cancelTask(@PathVariable taskId: UUID): ResponseEntity<Boolean> {
        val listener = listOf(segmentedVideoTaskListener, linearVideoTaskListener, subtitleTaskListener)
            .find { it.currentTaskId == taskId }

        val canceled = if (listener != null) {
            listener.currentJob?.cancel()
            true
        } else false

        return if (canceled)
            ResponseEntity.ok(true)
        else
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(false)
    }

}