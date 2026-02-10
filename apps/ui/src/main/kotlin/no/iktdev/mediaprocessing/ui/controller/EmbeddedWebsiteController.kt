package no.iktdev.mediaprocessing.ui.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.NoHandlerFoundException

@Controller
class EmbeddedWebsiteController {

    // Root fallback
    @GetMapping("/")
    fun root(): String {
        val index = ClassPathResource("static/index.html")
        return if (index.exists()) "forward:/index.html" else "forward:/noop"
    }

    @ExceptionHandler(NoHandlerFoundException::class)
    fun forwardToSpa(request: HttpServletRequest, ex: NoHandlerFoundException): String {
        val uri = request.requestURI

        // La API og filer få vanlig 404
        if (uri.startsWith("/api") || uri.contains(".")) {
            throw ex
        }

        // Alt annet → SPA
        return "forward:/index.html"
    }
}

