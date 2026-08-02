package no.iktdev.mediaprocessing.ui.client

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mediaprocessing.apps")
data class MediaProcessingAppsProperties(
    val coordinator: AppConfig,
    val processer: AppConfig,
    val converter: AppConfig,
    val metadata: AppConfig,
    val watcher: AppConfig
) {
    data class AppConfig(
        val address: String,
        val health: String
    )
}