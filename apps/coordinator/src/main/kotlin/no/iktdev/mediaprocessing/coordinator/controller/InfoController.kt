package no.iktdev.mediaprocessing.coordinator.controller

import no.iktdev.mediaprocessing.coordinator.Coordinator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class InfoController {
    @Autowired lateinit var coordinator: Coordinator

    @GetMapping("/cachedReferenceList")
    fun cachedReferenceList(): String {
        return coordinator.cachedReferenceList.joinToString(",")
    }
}