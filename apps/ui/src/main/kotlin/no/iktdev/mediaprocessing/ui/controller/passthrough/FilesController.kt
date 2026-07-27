package no.iktdev.mediaprocessing.ui.controller.passthrough

import no.iktdev.mediaprocessing.shared.common.dto.InputFileInfo
import no.iktdev.mediaprocessing.ui.service.coordinator.CoordinatorEventService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/files")
class FilesController(
    private val coordinator: CoordinatorEventService,
) {

    @GetMapping("/used")
    fun getFilesUsedInEvents(): Mono<List<InputFileInfo>> {
        return coordinator.getFilesUsedInEvents()
    }

    @PutMapping("/preserve")
    fun setPreservedFiles(@RequestBody files: List<String>): Mono<List<InputFileInfo>> {
        return coordinator.setPreservedFiles(files)
    }


}