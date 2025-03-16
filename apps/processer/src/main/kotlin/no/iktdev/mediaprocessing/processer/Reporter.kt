package no.iktdev.mediaprocessing.processer

import mu.KotlinLogging
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.contract.dto.ProcesserEventInfo
import no.iktdev.mediaprocessing.shared.common.task.Task
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
        try {
            messageTemplate.convertAndSend("/topic/encode/assigned", task)
        } catch (e: Exception) {
            //log.error { e.message }
        }
    }

    fun extractTaskAssigned(task: Task) {
        try {
            messageTemplate.convertAndSend("/topic/extract/assigned", task)
        } catch (e: Exception) {
            //log.error { e.message }
        }
    }

    fun sendEncodeProgress(progress: ProcesserEventInfo) {
        try {
            restTemplate.postForEntity(SharedConfig.uiUrl + "/encode/progress", progress, String::class.java)
            messageTemplate.convertAndSend("/topic/encode/progress", progress)
        } catch (e: Exception) {
            //log.error { e.message }
        }
    }

    fun sendExtractProgress(progress: ProcesserEventInfo) {
        try {
            restTemplate.postForEntity(SharedConfig.uiUrl + "/extract/progress", progress, String::class.java)
            messageTemplate.convertAndSend("/topic/extract/progress", progress)
        } catch (e: Exception) {
            //log.error { e.message }
        }
    }

}