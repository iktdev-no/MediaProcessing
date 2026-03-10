package no.iktdev.mediaprocessing.ui.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping


import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class SpaErrorController : ErrorController {

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest): String {
        val uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)?.toString() ?: ""

        if (!uri.startsWith("/api") && !uri.contains(".")) {
            return "forward:/index.html"
        }

        return "error"
    }
}


