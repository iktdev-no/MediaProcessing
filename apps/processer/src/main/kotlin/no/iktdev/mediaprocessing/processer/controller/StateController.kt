package no.iktdev.mediaprocessing.processer.controller

import mu.KotlinLogging
import no.iktdev.eventi.models.Progress
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/state")
class StateController(
    private val localProgress: LocalProgressCache
) {
    val log = KotlinLogging.logger {}


    @GetMapping("/progress")
    fun allProgress(): Map<UUID, ProgressUpdate> =
        localProgress.getAll()

    @GetMapping("/progress/{taskId}")
    fun progress(@PathVariable taskId: UUID): ProgressUpdate? =
        localProgress.get(taskId)


    @GetMapping("/log")
    fun getLog(@RequestParam path: String): ResponseEntity<String> {
        val file = IFile(path)
        log.info { "Attempting to find file $path" }
        return if (file.exists()) {
            log.info { "Found file $file" }
            ResponseEntity.ok(file.readText())
        } else {
            log.info { "File $path not found" }
            ResponseEntity.notFound().build()
        }
    }


}
