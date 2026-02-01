package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.processer.config.DirectoryProperties
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths

object TestUtils {
    fun getFileUtil(): FileUtil {
        val dirs = DirectoryProperties(
            logs = "build/test-logs",
        )
        val mediaPaths = MediaPaths(
            cache = "build/test-cache",
            incoming = "build/test-input",
            outgoing = "build/test-output"
        )

        return FileUtil(dirs, mediaPaths)
    }

    fun getExecutableConfig(): ExecutablesConfig {
        return ExecutablesConfig(
            ffmpeg = "ffmpeg"
        )
    }

}