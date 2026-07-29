package no.iktdev.mediaprocessing.processer.config

import no.iktdev.files.IFile
import no.iktdev.files.IFile.Companion.invoke
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "processer")
data class ProcesserProperties(
    val allowOverwrite: Boolean,
    val enableSegmentedTaskListener: Boolean,
    val preference: IFile = IFile("/data/config/preference.json")
)