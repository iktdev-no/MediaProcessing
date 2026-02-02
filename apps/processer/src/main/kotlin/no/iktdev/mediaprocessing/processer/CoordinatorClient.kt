package no.iktdev.mediaprocessing.processer

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class CoordinatorClient(
    private val processerProperties: ProcesserProperties,
    private val webClient: WebClient
) {
    private val log = KotlinLogging.logger {}

    @PostConstruct
    fun pingCoordinator() {
        if (!processerProperties.coordinatorPingOnStartup) {
            log.info { "Coordinator ping on startup is disabled" }
            return
        }
        try {
            val result = webClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            log.info { "Coordinator reachable. Health: $result" }
        } catch (e: Exception) {
            log.error(e) { "Coordinator NOT reachable at startup" }
        }
    }

    fun reportProgress(referenceId: String, taskId: String, percent: FfmpegDecodedProgress, message: String?) =
        webClient.post()
            .uri("/internal/progress")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(ProgressUpdate(referenceId, taskId, percent, message))
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess { log.info { "Progress sent" } }
            .doOnError { e -> log.error(e) { "Failed to send progress" } }
            .subscribe()

}
