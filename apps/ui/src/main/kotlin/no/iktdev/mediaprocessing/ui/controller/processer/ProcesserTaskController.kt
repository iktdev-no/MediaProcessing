package no.iktdev.mediaprocessing.ui.controller.processer

import no.iktdev.mediaprocessing.ui.client.ProcesserClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/processer")
class ProcesserTaskController(
    private val processerClient: ProcesserClient
) {

    @GetMapping("/log")
    fun getLog(@RequestParam path: String) = processerClient.fetchLog(path)

    @PostMapping("/tasks/{taskId}/cancel")
    fun cancelTask(@PathVariable taskId: UUID) = processerClient.cancelTask(taskId)

}