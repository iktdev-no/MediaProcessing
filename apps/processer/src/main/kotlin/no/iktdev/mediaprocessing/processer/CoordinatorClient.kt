package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.ffmpeg.decoder.FfmpegDecodedProgress
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class CoordinatorClient(
    private val webClient: WebClient
) {

    fun reportProgress(referenceId: String, taskId: String, percent: FfmpegDecodedProgress, message: String?) =
        webClient.post()
            .uri("/internal/progress")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                ProgressUpdate(referenceId, taskId, percent, message)
            )
            .retrieve()
            .toBodilessEntity()

}
