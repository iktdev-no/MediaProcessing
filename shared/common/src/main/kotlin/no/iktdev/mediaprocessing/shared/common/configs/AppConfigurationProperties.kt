package no.iktdev.mediaprocessing.shared.common.configs

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "streamit")
data class StreamItConfig(
    val address: String
)

@ConfigurationProperties(prefix = "media")
data class MediaPaths(
    val scratch: String,
    val intermediate: String,
    val outbox: String,
    val inbox: String
)
