package no.iktdev.mediaprocessing.ui.controller

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class EmbeddedWebsiteController {

    // Root fallback
    @GetMapping("/")
    fun root(): String {
        val index = ClassPathResource("static/index.html")
        return if (index.exists()) "forward:/index.html" else "forward:/noop"
    }

    // Fallback for React Router paths
    @GetMapping("/{path:[^\\.]*}")
    fun forward(path: String): String {
        val index = ClassPathResource("static/index.html")
        return if (index.exists()) "forward:/index.html" else "forward:/noop"
    }
}



