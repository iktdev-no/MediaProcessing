package no.iktdev.mediaprocessing.ui.service

import no.iktdev.mediaprocessing.shared.common.sse.SSEKeys
import no.iktdev.mediaprocessing.ui.AppConfig
import no.iktdev.mediaprocessing.ui.AppsConfig
import no.iktdev.mediaprocessing.ui.client.WebClientFactory
import no.iktdev.mediaprocessing.ui.models.contract.SystemStatus
import no.iktdev.mediaprocessing.ui.models.contract.sse.SSEHealthStatus
import no.iktdev.mediaprocessing.ui.service.sse.SSEListeningService
import no.iktdev.mediaprocessing.ui.service.sse.SSEServer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class StatusService(
    private val apps: AppsConfig,
    private val sse: SSEServer,
    private val webClientFactory: WebClientFactory,
    private val sseListeningService: SSEListeningService
) {
    var status: SystemStatus = SystemStatus()
    private val intervalMs = 5000L // samme som @Scheduled(fixedDelay = 5000)

    @Scheduled(fixedDelay = 5000)
    fun checkServices() {
        status.interval = intervalMs
        status.processer = check(apps.processer)
        status.converter = check(apps.converter)
        status.pyMetadata = check(apps.metadata)
        status.pyWatcher = check(apps.watcher)
        status.timestamp = System.currentTimeMillis()

        // coordinator REST sjekkes også her
        status.coordinatorRest = check(apps.coordinator)
        status.coordinatorSse = sseListeningService.isCoordinatorOnline()
        status.processerSse = sseListeningService.isProcesserOnline()
        sse.broadcast(SSEHealthStatus(status))
    }

    private fun check(app: AppConfig): Boolean =
        try {
            val client = webClientFactory.create(app.address)
            client.get()
                .uri(app.address + app.health)
                .retrieve()
                .bodyToMono(String::class.java)
                .timeout(Duration.ofSeconds(2))
                .map { true }
                .onErrorReturn(false)
                .block()!!
        } catch (ex: Exception) {
            false
        }
}
