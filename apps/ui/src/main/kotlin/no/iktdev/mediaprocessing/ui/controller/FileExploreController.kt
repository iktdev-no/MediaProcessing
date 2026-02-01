package no.iktdev.mediaprocessing.ui.controller

import no.iktdev.mediaprocessing.ui.MediaConfig
import no.iktdev.mediaprocessing.ui.dto.file.IFile
import no.iktdev.mediaprocessing.ui.service.ExplorerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File

@RestController
@RequestMapping("/api/files")
class FileExploreController(
    private val mediaConfig: MediaConfig,
    private val explorer: ExplorerService
) {

    @GetMapping("/home")
    fun home(): ResponseEntity<List<IFile>> {
        return ResponseEntity.ok(explorer.listHome())
    }

    @GetMapping("/roots")
    fun roots(): ResponseEntity<List<IFile>> {
        return ResponseEntity.ok(
            listOfNotNull(
                explorer.pathToFile(mediaConfig.incoming),
                explorer.pathToFile(mediaConfig.cache),
                explorer.pathToFile(mediaConfig.outgoing)
            )
        )
    }

    @GetMapping("/explore")
    fun list(@RequestParam path: String): ResponseEntity<List<IFile>> {
        val file = File(path)
        if (!file.exists() || file.isFile) {
            return ResponseEntity.notFound().build()
        }
        val files = explorer.listAt(path)
        return ResponseEntity.ok(files)
    }
}