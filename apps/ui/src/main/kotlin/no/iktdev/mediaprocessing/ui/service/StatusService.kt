package no.iktdev.mediaprocessing.ui.service

import jakarta.annotation.PostConstruct
import no.iktdev.mediaprocessing.ui.AppConfig
import no.iktdev.mediaprocessing.ui.AppsConfig
import no.iktdev.mediaprocessing.ui.UiSseHub
import no.iktdev.mediaprocessing.ui.dto.status.SystemStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Service
class StatusService(
    private val apps: AppsConfig,
    @param:Qualifier("coordinatorWebClient") private val webClient: WebClient,
    private val hub: UiSseHub
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
        hub.broadcast(status, "healthStatus")
    }

    @PostConstruct
    fun init() {
        hub.registerListener(object : UiSseHub.SSEStateListener {
            override fun onConnected() {
                status.coordinatorSse = true
            }

            override fun onReconnecting() {
                status.coordinatorSse = false
            }

            override fun onDisconnected() {
                status.coordinatorSse = false
            }
        })
    }


    private fun check(app: AppConfig): Boolean =
        try {
            webClient.get()
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
