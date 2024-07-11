package no.iktdev.mediaprocessing.coordinator.controller

import com.google.gson.Gson
import no.iktdev.mediaprocessing.shared.contract.ProcessType
import no.iktdev.mediaprocessing.shared.contract.dto.EventRequest
import no.iktdev.mediaprocessing.shared.contract.dto.StartOperationEvents
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
class RequestEventController(@Autowired var coordinator: EventCoordinatorDep) {

    @PostMapping("/convert")
    @ResponseStatus(HttpStatus.OK)
    fun requestConvert(@RequestBody payload: String): ResponseEntity<String> {
        var convert: EventRequest? = null
        var referenceId: String?
        try {
            convert = Gson().fromJson(payload, EventRequest::class.java)
            val file = File(convert.file)
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(convert.file)
            }
            referenceId = coordinator.startProcess(file, ProcessType.FLOW, listOf(StartOperationEvents.CONVERT)).toString()

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Gson().toJson(convert))
        }
        return ResponseEntity.ok(referenceId)
    }

    @PostMapping("/extract")
    @ResponseStatus(HttpStatus.OK)
    fun requestExtract(@RequestBody payload: String): ResponseEntity<String> {
        var request: EventRequest? = null
        var referenceId: String?
        try {
            request = Gson().fromJson(payload, EventRequest::class.java)
            val file = File(request.file)
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(payload)
            }
            referenceId = coordinator.startProcess(file, ProcessType.MANUAL, listOf(StartOperationEvents.EXTRACT)).toString()

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload)
        }
        return ResponseEntity.ok(referenceId)
    }

    @PostMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    fun requestAll(@RequestBody payload: String): ResponseEntity<String> {
        var request: EventRequest? = null
        var referenceId: String?
        try {
            request = Gson().fromJson(payload, EventRequest::class.java)
            val file = File(request.file)
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(payload)
            }
            referenceId = coordinator.startProcess(file, type = ProcessType.MANUAL).toString()

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload)
        }
        return ResponseEntity.ok(referenceId)
    }
}