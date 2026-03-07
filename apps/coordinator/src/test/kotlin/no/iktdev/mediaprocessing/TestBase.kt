package no.iktdev.mediaprocessing

import io.mockk.*
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.registry.EventTypeRegistry
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ContinuationSummaryEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OperationType
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartFlow
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.StartProcessingEvent
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan
import no.iktdev.mediaprocessing.shared.common.model.MediaType
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


    fun defaultStartEvent(flow: StartFlow = StartFlow.Auto): StartProcessingEvent {
        val start = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.Encode, OperationType.ExtractSubtitles, OperationType.ConvertSubtitles, OperationType.MetadataSearch),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv",
                flow = flow
            )
        ).apply { newReferenceId() }
        return start
    }

    fun defaultSummaryEvent() = ContinuationSummaryEvent(
        data = ContentExport(
            "Baking Bread",
            episodeInfo = ContentExport.EpisodeInfo(1, 1, "Flour"),
            metadata = ContentExport.MetadataExport(
                "Baking Bread",
                listOf("Bread in the making"),
                listOf("Comedy", "Baking"),
                "Baking Bread.jpg",
                summary = emptyList(),
                mediaType = MediaType.Serie,
                source = "The Cook book"
            ),
            media = ContentExport.MediaExport(
                videoFile = "Baking Bread - S01E01 - Flour.mp4",
                subtitles = listOf(
                    ContentExport.MediaExport.Subtitle(
                        subtitleFile = "Baking Bread - S01E01 - Flour.ass",
                        language = "eng",
                    )
                )
            )
        ),
        plan = ContentMigrationPlan(
            collection = "Baking Bread",
            videoContent = ContentMigrationPlan.SingleContent("cached:///bakingbread/Baking Bread - S01E01 - Flour.mp4", "store:///Baking bread/Baking Bread - S01E01 - Flour.mp4"),
            coverContent = ContentMigrationPlan.SingleContent("cached:///bakingbread/Baking Bread.jpg", "store:///Baking bread/Baking Bread.jpg"),
            subtitleContent = listOf(
                ContentMigrationPlan.SingleSubtitle(
                    language = "eng",
                    cachedUri = "cached:///bakingbread/Baking Bread - S01E01 - Flour.ass",
                    storeUri = "store:///Baking bread/sub/eng/Baking Bread - S01E01 - Flour.mp4"
                )
            )
        )
    )

    fun Event.addToHistory(): Event {
        history.add(this)
        eventStore.persist(this)
        return this
    }

    fun List<Event>.addToHistory(): List<Event> {
        this.forEach { event -> event.addToHistory() }
        return this
    }

}