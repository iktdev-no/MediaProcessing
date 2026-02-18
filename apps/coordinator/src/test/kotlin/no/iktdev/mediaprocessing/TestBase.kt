package no.iktdev.mediaprocessing

import io.mockk.*
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.registry.EventTypeRegistry
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.database.InMemoryEventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.ProcesserPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.VideoPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioCodecType
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.audio.AudioPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.video.VideoCodecType
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.*

open class TestBase {
    val eventStore = InMemoryEventStore()

    val history = mutableListOf<Event>()

    class DummyEvent: Event()
    class DummyTask: Task()

    val preference: Preference = mockk(relaxed = true)
    val coordinatorEnv = mockk<CoordinatorEnv>(relaxed = true)

    val defaultVideoPreference = VideoPreference(VideoCodecConfig(VideoCodecType.HEVC))
    val defaultAudioPreference = AudioPreference(AudioCodecConfig(AudioCodecType.AAC).copy(channels = 2))

    @BeforeEach
    open fun setup() {
        mockkObject(TaskStore)
        every { TaskStore.persist(any()) } returns true
        every { preference.getProcesserPreference() } returns ProcesserPreference(
            videoPreference = defaultVideoPreference,
            audioPreference = defaultAudioPreference
        )
        every { coordinatorEnv.outboxFolder } returns File("./tmp/outbox")
        every { coordinatorEnv.inboxFolder } returns File("./tmp/inbox")
        every { coordinatorEnv.scratchFolder } returns File("./tmp/scratch")
        every { coordinatorEnv.intermediateFolder } returns File("./tmp/intermediate")
        every { coordinatorEnv.streamitAddress } returns "http://streamit.lan"

        EventRegistry.getEvents().let {
            EventTypeRegistry.register(it)
        }
        eventStore.clear()
        history.clear()
    }


    fun defaultStartEvent(): StartProcessingEvent {
        val start = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.Encode, OperationType.ExtractSubtitles, OperationType.ConvertSubtitles, OperationType.MetadataSearch),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        ).apply { newReferenceId() }
        return start

    }

    fun Event.addToHistory(): Event {
        history.add(this)
        return this
    }

}