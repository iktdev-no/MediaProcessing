package no.iktdev.mediaprocessing

import io.mockk.*
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.mediaprocessing.coordinator.AudioPreference
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.coordinator.ProcesserPreference
import no.iktdev.mediaprocessing.coordinator.VideoPreference
import no.iktdev.mediaprocessing.ffmpeg.dsl.AudioCodec
import no.iktdev.mediaprocessing.ffmpeg.dsl.VideoCodec
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.stores.TaskStore
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.*

open class TestBase {
    class DummyEvent: Event()
    class DummyTask: Task()

    @BeforeEach
    fun setup() {
        mockkObject(TaskStore)
        every { TaskStore.persist(any()) } just Runs
        mockkObject(Preference)
        every { Preference.getProcesserPreference() } returns ProcesserPreference(
            videoPreference = VideoPreference(codec = VideoCodec.Hevc()),
            audioPreference = AudioPreference(codec = AudioCodec.Aac(channels = 2))
        )
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