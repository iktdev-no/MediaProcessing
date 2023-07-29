package no.iktdev.streamit.content.encode.controllers

import com.google.gson.Gson
import no.iktdev.streamit.content.encode.progressMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class ProgressController {
    @GetMapping("/progress")
    fun getValue(response: HttpServletResponse): String {
        response.setHeader("Refresh", "5")
        return Gson().toJson(progressMap.values)
    }
}
