package no.iktdev.mediaprocessing.coordinator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "executables")
data class ExecutablesConfig(
    val ffprobe: String
)