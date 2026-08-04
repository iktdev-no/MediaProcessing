package no.iktdev.mediaprocessing.ui.controller

import mu.KotlinLogging
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.ui.MediaConfig
import no.iktdev.mediaprocessing.ui.models.contract.files.UiFile
import no.iktdev.mediaprocessing.ui.models.contract.requests.DeleteRequest
import no.iktdev.mediaprocessing.ui.models.contract.files.PreservedFile
import no.iktdev.mediaprocessing.ui.models.contract.files.translate
import no.iktdev.mediaprocessing.ui.service.ExplorerService
import no.iktdev.mediaprocessing.ui.service.FilePreservationService
import no.iktdev.mediaprocessing.ui.toEvents
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

@RestController
@RequestMapping("/api/files")
class FilesController(
    private val mediaConfig: MediaConfig,
    private val explorer: ExplorerService,
    private val fps: FilePreservationService,
) {
    private val log = KotlinLogging.logger {}


    @GetMapping("/used")
    fun getFilesUsedInEvents(): List<PreservedFile> {
        return fps.getFilesWithPreservedInputFiles().map { it.translate() }
    }

    @PutMapping("/preserve")
    fun setPreservedFiles(@RequestBody files: List<String>): List<PreservedFile> {
        return fps.setFilesAsPreserved(files).map { it.translate() }
    }

    @GetMapping("/home")
    fun home(): ResponseEntity<List<UiFile>> {
        return ResponseEntity.ok(explorer.listHome())
    }

    @GetMapping("/roots")
    fun roots(): ResponseEntity<List<UiFile>> {
        return ResponseEntity.ok(
            listOfNotNull(
                explorer.pathToFile(mediaConfig.inbox),
                explorer.pathToFile(mediaConfig.scratch),
                explorer.pathToFile(mediaConfig.intermediate),
                explorer.pathToFile(mediaConfig.outbox)
            )
        )
    }


    @GetMapping("/explore")
    fun list(@RequestParam path: String, @RequestParam new: Boolean = false): ResponseEntity<List<UiFile>> {
        val file = File(path)
        if (!file.exists() || file.isFile) {
            return ResponseEntity.notFound().build()
        }
        val files = if (new) {
            val exclude = getAlreadyInSystem()
            explorer.listAt(path).filter { it.name !in exclude }
        } else explorer.listAt(path)
        return ResponseEntity.ok(files)
    }

    private var lastUpdated: Instant = Instant.EPOCH
    private var alreadyInSystemCache: List<String> = emptyList()
    private fun getAlreadyInSystem(): List<String> {
        if (alreadyInSystemCache.isEmpty() || Duration.between(lastUpdated, Instant.now()).toMinutes() > 5) {
            val inSystem = EventStore.getFilesInSystem().map { IFile(it).name }
            alreadyInSystemCache = inSystem
            lastUpdated = Instant.now()
        }
        return alreadyInSystemCache
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
            mediaConfig.inbox,
            mediaConfig.scratch,
            mediaConfig.intermediate,
            mediaConfig.outbox
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