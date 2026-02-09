package no.iktdev.mediaprocessing.processer.listeners

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.tasks.TaskType
import no.iktdev.mediaprocessing.processer.WorkingFile
import no.iktdev.mediaprocessing.processer.config.ProcesserProperties
import no.iktdev.mediaprocessing.processer.strategy.EncodingStrategy
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.EncodeTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class VideoTaskListenerTest {

    private val props = ProcesserProperties(
        coordinatorUrl = "http://localhost",
        coordinatorPingOnStartup = false,
        allowOverwrite = true,
        enableSegmentedTaskListener = true
    )


    private val listener = object : VideoTaskListener(TaskType.CPU_INTENSIVE, props) {
        override fun getWorkerId(): String {
            return UUID.randomUUID().toString()
        }

        override suspend fun onTask(task: Task): Event? {
            return null
        }
    }

    private fun taskWithArgs(vararg args: String): EncodeTask {
        return EncodeTask(
            data = EncodeData(
                inputFile = WorkingFile("Test-in.mvk").absolutePath,
                outputFileName = WorkingFile("Test-out.mp4").absolutePath,
                arguments = args.toList()
            )
        ).apply { newReferenceId() }
    }

    @Test
    @DisplayName("""
        Når encode-argumentene kun inneholder copy
        Hvis getEncodeStrategy() kalles
        Så:
            Skal Linear returneres
    """)
    fun copy_returns_linear() {
        val task = taskWithArgs("-c", "copy")
        assertEquals(EncodingStrategy.Linear, listener.getEncodeStrategy(task))
    }

    @Test
    @DisplayName("""
        Når encode-argumentene kun påvirker audio
        Hvis getEncodeStrategy() kalles
        Så:
            Skal Linear returneres
    """)
    fun audio_only_returns_linear() {
        val task = taskWithArgs("-c:a", "aac")
        assertEquals(EncodingStrategy.Linear, listener.getEncodeStrategy(task))
    }

    @Test
    @DisplayName("""
        Når encode-argumentene påvirker video
        Hvis getEncodeStrategy() kalles
        Så:
            Skal Segmented returneres
    """)
    fun video_reencode_returns_segmented() {
        val task = taskWithArgs("-c:v", "libx264")
        assertEquals(EncodingStrategy.Segmented, listener.getEncodeStrategy(task))
    }

    @Test
    @DisplayName("""
        Når encode-argumentene inneholder filtergraph
        Hvis getEncodeStrategy() kalles
        Så:
            Skal Segmented returneres
    """)
    fun filtergraph_returns_segmented() {
        val task = taskWithArgs("-vf", "scale=1920:1080")
        assertEquals(EncodingStrategy.Segmented, listener.getEncodeStrategy(task))
    }

    @Test
    @DisplayName("""
        Når encode-argumentene inneholder concat
        Hvis getEncodeStrategy() kalles
        Så:
            Skal Linear returneres
    """)
    fun concat_returns_linear() {
        val task = taskWithArgs("-f", "concat")
        assertEquals(EncodingStrategy.Linear, listener.getEncodeStrategy(task))
    }
}
