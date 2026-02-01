package no.iktdev.mediaprocessing.processer.config

import org.jetbrains.annotations.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "directories")
@Validated
data class DirectoryProperties(
    @field:NotNull
    val logs: String,
)
