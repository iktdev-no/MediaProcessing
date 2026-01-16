package no.iktdev.mediaprocessing

import io.mockk.*
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.coordinator.*
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore

import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.*

open class TestBase {
    class DummyEvent: Event()
    class DummyTask: Task()

    val preference: Preference = mockk(relaxed = true)
    val coordinatorEnv = mockk<CoordinatorEnv>(relaxed = true)


    @BeforeEach
    open fun setup() {
        mockkObject(TaskStore)
        every { TaskStore.persist(any()) } just Runs
        every { preference.getProcesserPreference() } returns ProcesserPreference(
            videoPreference = VideoPreference(codec = VideoCodec.Hevc()),
            audioPreference = AudioPreference(codec = AudioCodec.Aac(channels = 2))
        )
        every { coordinatorEnv.outgoingContent } returns File("./tmp/output")
        every { coordinatorEnv.incomingContent } returns File("./tmp/input")
        every { coordinatorEnv.cachedContent } returns File("./tmp/cached")
        every { coordinatorEnv.streamitAddress } returns "http://streamit.lan"
    }


    fun mockkIO() {
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
    }

    fun defaultStartEvent(): StartProcessingEvent {
        val start = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.Encode, OperationType.Extract, OperationType.Convert),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        )
        start.newReferenceId()
        return start

    }
}