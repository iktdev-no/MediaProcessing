package no.iktdev.mediaprocessing

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.registry.EventTypeRegistry
import no.iktdev.files.FakeFile
import no.iktdev.files.IFile
import no.iktdev.mediaprocessing.coordinator.CoordinatorEnv
import no.iktdev.mediaprocessing.coordinator.Preference
import no.iktdev.mediaprocessing.shared.common.event_task_contract.EventRegistry
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.database.InMemoryEventStore
import no.iktdev.mediaprocessing.shared.database.stores.TaskStore
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.MediaPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.video.VideoPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.audio.AudioCodecConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.audio.AudioCodecType
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.audio.AudioPreference
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.video.VideoCodecConfig
import no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator.video.VideoCodecType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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
        every { preference.getMediaPreference() } returns MediaPreference(
            videoPreference = defaultVideoPreference,
            audioPreference = defaultAudioPreference
        )
        every { coordinatorEnv.outboxFolder } returns IFile("./tmp/outbox")
        every { coordinatorEnv.inboxFolder } returns IFile("./tmp/inbox")
        every { coordinatorEnv.scratchFolder } returns IFile("./tmp/scratch")
        every { coordinatorEnv.intermediateFolder } returns IFile("./tmp/intermediate")
        every { coordinatorEnv.streamitAddress } returns "http://streamit.lan"

        EventRegistry.getEvents().let {
            EventTypeRegistry.register(it)
        }
        eventStore.clear()
        history.clear()
        FakeFile.wipe()
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
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

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupAtStart() {
            IFile.factory = { path -> FakeFile(path) }
            FakeFile.wipe()
        }
    }

}