package no.iktdev.mediaprocessing.processer.controller

import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.LocalProgressCache
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.util.*

@RestController
@RequestMapping("/state")
class StateController(
    private val localProgress: LocalProgressCache
) {

    @GetMapping("/progress")
    fun allProgress(): Map<UUID, FfmpegDecodedProgress> =
        localProgress.getAll()

    @GetMapping("/progress/{taskId}")
    fun progress(@PathVariable taskId: UUID): FfmpegDecodedProgress? =
        localProgress.get(taskId)


    @GetMapping("/log")
    fun getLog(@RequestParam path: String): ResponseEntity<String> {
        val file = File(path)
        return if (file.exists()) {
            ResponseEntity.ok(file.readText())
        } else {
            ResponseEntity.notFound().build()
        }
    }


}
