package no.iktdev.mediaprocessing.coordinator.controller

import com.google.gson.Gson
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.EventsManager
import no.iktdev.mediaprocessing.shared.contract.dto.RequestWorkProceed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping(path = ["/action"])
class ActionEventController(@Autowired var coordinator: Coordinator, @Autowired var eventsManager: EventsManager) {


    @RequestMapping("/flow/proceed")
    fun permitRunOnSequence(@RequestBody data: RequestWorkProceed): ResponseEntity<String> {

        val set = eventsManager.getEventsWith(data.referenceId)
        if (set.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(Gson().toJson(data))
        }
        coordinator.permitWorkToProceedOn(data.referenceId, set, "Requested by ${data.source}")

        //EVENT_MEDIA_WORK_PROCEED_PERMITTED("event:media-work-proceed:permitted")
        return ResponseEntity.ok(null)
    }
}