package no.iktdev.mediaprocessing.coordinator.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "preference")
data class AppConfig(
    val preservedFile: String,
    val preferenceFile: String
)