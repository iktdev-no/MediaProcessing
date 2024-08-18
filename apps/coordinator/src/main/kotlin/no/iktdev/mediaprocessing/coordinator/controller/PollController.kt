package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.Coordinator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping(path = ["/polls"])
class PollController(@Autowired var coordinator: Coordinator) {

    @GetMapping()
    fun polls(): String {
        val stat = coordinator.getActivePolls()
        return "Active Polls ${stat.active}/${stat.total}"
    }
}
