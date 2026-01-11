package no.iktdev.mediaprocessing.processer

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "executables")
data class ExecutablesConfig(
    val ffmpeg: String
)