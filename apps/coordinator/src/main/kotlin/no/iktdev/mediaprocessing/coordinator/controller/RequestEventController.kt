package no.iktdev.mediaprocessing.coordinator.controller

import com.google.gson.Gson
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.shared.common.contract.ProcessType
import no.iktdev.mediaprocessing.shared.common.contract.dto.EventRequest
import no.iktdev.mediaprocessing.shared.common.contract.dto.OperationEvents
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import java.io.File

@Controller
@RequestMapping(path = ["/request"])
class RequestEventController(@Autowired var coordinator: Coordinator) {

    @PostMapping("/convert")
    @ResponseStatus(HttpStatus.OK)
    fun requestConvert(@RequestBody payload: EventRequest): ResponseEntity<String> {
        var referenceId: String?
        try {
            val file = File(payload.file)
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(payload.file)
            }
            referenceId = coordinator.startProcess(file, payload.mode, listOf(OperationEvents.CONVERT)).toString()

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Gson().toJson(payload))
        }
        return ResponseEntity.ok(referenceId)
    }

    @PostMapping("/extract")
    @ResponseStatus(HttpStatus.OK)
    fun requestExtract(@RequestBody payload: EventRequest): ResponseEntity<String> {
        var referenceId: String?
        try {
            val file = File(payload.file)
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Gson().toJson(payload))
            }
            referenceId = coordinator.startProcess(file, payload.mode, listOf(OperationEvents.EXTRACT)).toString()

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Gson().toJson(payload))
        }
        return ResponseEntity.ok(referenceId)
    }

    @PostMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    fun requestAll(@RequestBody payload: EventRequest): ResponseEntity<String> {
        var referenceId: String?
        try {
            val file = File(payload.file)
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(payload.file)
            }
            referenceId = coordinator.startProcess(file, type = payload.mode).toString()

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Gson().toJson(payload))
        }
        return ResponseEntity.ok(referenceId)
    }
}