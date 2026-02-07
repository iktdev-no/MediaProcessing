package no.iktdev.mediaprocessing.processer

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.processer.config.DirectoryProperties
import no.iktdev.mediaprocessing.processer.config.ExecutablesConfig
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import org.junit.jupiter.api.Assertions.assertEquals

object TestUtils {
    fun getFileUtil(): FileUtil {
        val dirs = DirectoryProperties(
            logs = "build/test-logs",
        )
        val mediaPaths = MediaPaths(
            scratch = "build/test-scratch",
            intermediate = "build/test-intermediate",
            inbox = "build/test-inbox",
            outbox = "build/test-outbox"
        )

        return FileUtil(dirs, mediaPaths)
    }

    fun getExecutableConfig(): ExecutablesConfig {
        return ExecutablesConfig(
            ffmpeg = "ffmpeg"
        )
    }

}

fun assertSameReferenceId(task: Task, event: Event?) {
    requireNotNull(event) { "Event was null" }
    assertEquals(
        task.referenceId,
        event.referenceId,
        "Expected event to keep same referenceId as task"
    )
}
