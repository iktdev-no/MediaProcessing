package no.iktdev.mediaprocessing.coordinator.controller

import com.google.gson.Gson
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.shared.contract.dto.ConvertRequest
import no.iktdev.mediaprocessing.shared.contract.dto.RequestStartOperationEvents
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
    fun requestConvert(@RequestBody convert: ConvertRequest): ResponseEntity<String> {
        try {
            val file = File(convert.file)
            if (!file.exists()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(convert.file)
            }
            val referenceId = coordinator.startRequestProcess(file, listOf(RequestStartOperationEvents.CONVERT))

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Gson().toJson(convert))
        }
        return ResponseEntity.ok(null)
    }
}