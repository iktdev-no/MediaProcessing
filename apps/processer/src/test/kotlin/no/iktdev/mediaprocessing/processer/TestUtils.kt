package no.iktdev.mediaprocessing.processer

import io.mockk.every
import io.mockk.mockk
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.registry.TaskListenerRegistry
import no.iktdev.eventi.registry.TaskTypeRegistry
import no.iktdev.eventi.tasks.TaskListener
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.mediaprocessing.processer.config.DirectoryProperties
import no.iktdev.mediaprocessing.processer.config.FileUtil
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.shared.common.configs.MediaPaths
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import java.lang.reflect.Field

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


}

fun assertSameReferenceId(task: Task, event: Event?) {
    requireNotNull(event) { "Event was null" }
    assertEquals(
        task.referenceId,
        event.referenceId,
        "Expected event to keep same referenceId as task"
    )
}


fun getTaskReporter(): TaskReporter {
    val reporter = mockk<TaskReporter>(relaxed = true)

    every { reporter.markClaimed(any(), any()) } returns no.iktdev.eventi.tasks.Result.Success
    every { reporter.publishEvent(any()) } returns no.iktdev.eventi.tasks.Result.Success
    every { reporter.markCompleted(any()) } returns no.iktdev.eventi.tasks.Result.Success
    every { reporter.updateProgress(any(), any(), any()) } returns no.iktdev.eventi.tasks.Result.Success

    return reporter
}

fun getProcesserProperties() = ProcesserProperties(
    coordinatorUrl = "http://localhost",
    coordinatorPingOnStartup = false,
    allowOverwrite = true,
    enableSegmentedTaskListener = true
)

fun getCoordinatorClient() = mockk<CoordinatorClient>(relaxed = true)

@Suppress("UNUSED_RECEIVER_PARAMETER")
fun TaskTypeRegistry.wipe() {
    val field: Field = TaskTypeRegistry::class.java
        .superclass
        .getDeclaredField("types")
    field.isAccessible = true

    // Tøm map’en
    val typesMap = field.get(TaskTypeRegistry) as MutableMap<*, *>
    @Suppress("UNCHECKED_CAST")
    (typesMap as MutableMap<String, Class<out Task>>).clear()

    // Verifiser at det er tomt
    assertNull(TaskTypeRegistry.resolve("ANnonExistingEvent"))
}

fun TaskListenerRegistry.wipe() {
    val field: Field = TaskListenerRegistry::class.java
        .superclass
        .getDeclaredField("listeners")
    field.isAccessible = true

    // Tøm map’en
    val mutableList = field.get(TaskListenerRegistry) as MutableList<*>
    @Suppress("UNCHECKED_CAST")
    (mutableList as MutableList<Class<out TaskListener>>).clear()

    // Verifiser at det er tomt
    assertThat(TaskListenerRegistry.getListeners().isEmpty())
}