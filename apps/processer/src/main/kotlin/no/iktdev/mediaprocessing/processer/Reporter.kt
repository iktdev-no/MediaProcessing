package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.contract.dto.ProcesserEventInfo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class Reporter() {
    @Autowired
    lateinit var restTemplate: RestTemplate
    fun sendEncodeProgress(progress: ProcesserEventInfo) {
        try {
            restTemplate.postForEntity(SharedConfig.uiUrl + "/encode/progress", progress, String::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendExtractProgress(progress: ProcesserEventInfo) {
        try {
            restTemplate.postForEntity(SharedConfig.uiUrl + "/extract/progress", progress, String::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}