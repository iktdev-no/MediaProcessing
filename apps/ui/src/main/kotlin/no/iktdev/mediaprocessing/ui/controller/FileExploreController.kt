package no.iktdev.mediaprocessing.ui.controller

import mu.KotlinLogging
import no.iktdev.mediaprocessing.ui.MediaConfig
import no.iktdev.mediaprocessing.ui.dto.file.IFile
import no.iktdev.mediaprocessing.ui.dto.requests.DeleteRequest
import no.iktdev.mediaprocessing.ui.service.ExplorerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File

@RestController
@RequestMapping("/api/files")
class FileExploreController(
    private val mediaConfig: MediaConfig,
    private val explorer: ExplorerService
) {
    private val log = KotlinLogging.logger {}


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


    @DeleteMapping("/delete")
    fun delete(@RequestBody req: DeleteRequest): ResponseEntity<Void> {
        val file = File(req.uri)

        // 1. Eksisterer filen?
        if (!file.exists()) {
            log.warn { "Delete failed: ${req.uri} does not exist" }
            return ResponseEntity.notFound().build()
        }

        // 2. Sikkerhet: sjekk at path er innenfor allowed roots
        val allowedRoots = listOf(
            mediaConfig.incoming,
            mediaConfig.cache,
            mediaConfig.outgoing
        ).map { File(it).absoluteFile }

        val canonical = file.canonicalFile
        val isAllowed = allowedRoots.any { root ->
            canonical.path.startsWith(root.path)
        }

        if (!isAllowed) {
            log.error { "Attempted delete outside allowed roots: ${canonical.path}" }
            return ResponseEntity.status(403).build()
        }

        // 3. Sletting
        try {
            val deleted = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            if (!deleted) {
                throw Exception("Failed to delete file: ${file.absolutePath}")
            }
            log.info { "Deleted: ${canonical.path}" }
            return ResponseEntity.noContent().build()
        } catch (e: Exception) {
            log.error(e) { "Failed to delete ${canonical.path}" }
            return ResponseEntity.internalServerError().build()
        }
    }
}