package no.iktdev.mediaprocessing.processer.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "processer")
data class ProcesserProperties(
    val coordinatorUrl: String,
    val coordinatorPingOnStartup: Boolean,
    val allowOverwrite: Boolean
)