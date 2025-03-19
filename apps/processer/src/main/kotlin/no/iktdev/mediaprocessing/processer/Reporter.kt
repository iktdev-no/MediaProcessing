package no.iktdev.mediaprocessing.processer

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.shared.common.task.Task
import no.iktdev.mediaprocessing.shared.common.tryPost
import no.iktdev.mediaprocessing.shared.common.trySend
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class Reporter() {
    @Autowired
    lateinit var restTemplate: RestTemplate
    @Autowired
    lateinit var messageTemplate: SimpMessagingTemplate

    private val log = KotlinLogging.logger {}


    fun encodeTaskAssigned(task: Task) {
        messageTemplate.trySend("/topic/encode/assigned", task)
    }

    fun extractTaskAssigned(task: Task) {
        messageTemplate.trySend("/topic/extract/assigned", task)
    }

    fun sendEncodeProgress(progress: ProcesserEventInfo) {
        restTemplate.tryPost<String>(SharedConfig.uiUrl + "/encode/progress", progress)
        messageTemplate.trySend("/topic/encode/progress", progress)
    }

    fun sendExtractProgress(progress: ProcesserEventInfo) {
        restTemplate.tryPost<String>(SharedConfig.uiUrl + "/extract/progress", progress)
        messageTemplate.trySend("/topic/extract/progress", progress)
    }

}