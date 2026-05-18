package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.services.FileInfoService
import no.iktdev.mediaprocessing.shared.common.dto.FileTableItem
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.database.queries.FilesTableQueries
import no.iktdev.mediaprocessing.shared.database.stores.EventStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.InputFileInfo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/files")
class FilesController(
    val fileInfoService: FileInfoService,
) {

    @GetMapping()
    fun getFilesInDatabase(): ResponseEntity<List<FileTableItem>?>? {
        val files = FilesTableQueries().getFiles()
        return ResponseEntity.ok(files)
    }

    @GetMapping("/used")
    fun getFilesUsedInEvents(): List<InputFileInfo> {
        return fileInfoService.getFilesWithPreservedInputFiles()
    }

    @PutMapping("/preserve")
    fun setPreservedFiles(@RequestBody files: List<String>): List<InputFileInfo> {
        return fileInfoService.setFilesAsPreserved(files)
    }


}