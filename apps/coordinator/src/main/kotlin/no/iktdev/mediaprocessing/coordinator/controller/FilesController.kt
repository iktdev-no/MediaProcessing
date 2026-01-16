package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.shared.common.dto.FileTableItem
import no.iktdev.mediaprocessing.shared.database.queries.FilesTableQueries
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/files")
class FilesController {

    @GetMapping()
    fun getFilesInDatabase(): ResponseEntity<List<FileTableItem>?>? {
        val files = FilesTableQueries().getFiles()
        return ResponseEntity.ok(files)
    }
}