package no.iktdev.mediaprocessing.ui.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping


@Controller
class EmbeddedWebsiteController {

    @GetMapping("/{path:[^\\.]*}")
    fun forwardSingle(): String = "forward:/index.html"
}



