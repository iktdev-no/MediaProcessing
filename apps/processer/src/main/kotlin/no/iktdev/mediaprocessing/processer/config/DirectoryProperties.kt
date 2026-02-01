package no.iktdev.mediaprocessing.processer.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "directories")
data class DirectoryProperties(
    val logs: String,
)