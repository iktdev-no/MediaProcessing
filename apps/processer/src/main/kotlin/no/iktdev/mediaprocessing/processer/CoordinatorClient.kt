package no.iktdev.mediaprocessing.processer

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import no.iktdev.eventi.models.Progress
import no.iktdev.eventi.serialization.ZPS.toEnvelope
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

        val maxAttempts = 3
        var attempt = 1
        var delayMs = 500L

        while (attempt <= maxAttempts) {
            try {
                log.info { "Pinging coordinator (attempt $attempt/$maxAttempts)..." }

                val result = webClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .block()

                log.info { "Coordinator reachable. Health: $result" }
                return
            } catch (e: Exception) {
                log.warn(e) { "Coordinator ping failed on attempt $attempt" }

                if (attempt == maxAttempts) {
                    log.error { "Coordinator NOT reachable after $maxAttempts attempts" }
                    return
                }

                Thread.sleep(delayMs)
                delayMs *= 2
                attempt++
            }
        }
    }


    fun reportProgress(referenceId: String, taskId: String, payload: Progress) =
        webClient.post()
            .uri("/internal/progress")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(ProgressUpdate(referenceId, taskId, payload.toEnvelope()))
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess { log.info { "Progress sent" } }
            .doOnError { e -> log.error(e) { "Failed to send progress" } }
            .subscribe()

}
