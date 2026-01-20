package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.services.CommandService
import no.iktdev.mediaprocessing.shared.common.dto.requests.StartProcessRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/operations")
class OperationsController(
    private val commandService: CommandService
) {

    @PostMapping("/start")
    fun startProcess(@RequestBody req: StartProcessRequest): ResponseEntity<Map<String, String>> {
        return when (val result = commandService.startProcess(req)) {
            is CommandService.StartResult.Accepted -> ResponseEntity
                .accepted()
                .body(
                    mapOf(
                        "referenceId" to result.referenceId.toString(),
                        "status" to "accepted",
                        "message" to "Process accepted and StartedEvent created"
                    )
                )

            is CommandService.StartResult.Rejected -> ResponseEntity
                .badRequest()
                .body(
                    mapOf(
                        "status" to "rejected",
                        "message" to result.reason
                    )
                )
        }
    }


}