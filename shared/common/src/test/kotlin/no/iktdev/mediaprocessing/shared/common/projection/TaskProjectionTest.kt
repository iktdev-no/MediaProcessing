package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.MultiTaskIdentity
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.ffmpeg.data.FFmpegInstructions
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.InputSection
import no.iktdev.mediaprocessing.ffmpeg.dsl.args.section.OutputSection
import no.iktdev.mediaprocessing.shared.common.TestBase
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.*
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleData
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ExtractSubtitleTask
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.MediaReadTask
import no.iktdev.mediaprocessing.shared.common.projection.tasks.TaskProjection
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.collections.emptyList

class TaskProjectionTest : TestBase() {

    @Test
    @DisplayName("""
        Når ingen events finnes
        Så:
            Skal projectStreamReadStatus være NotInitiated
    """)
    fun testChainAcknowledgementNotInitiated1() {
        val events: MutableList<Event> = mutableListOf()
        val projection = TaskProjection(events)
        assertThat(projection.projectStreamReadStatus()).isEqualTo(TaskStatus.NotInitiated)
    }

    @Test
    @DisplayName("""
        Når ReadStreamsTaskCreatedEvent og Completed-resultat finnes
        Så:
            Skal projectStreamReadStatus være Completed
    """)
    fun testChainAcknowledgementSuccess1() {

        val readTask = MediaReadTask(fileUri = "").newReferenceId()

        CoordinatorReadStreamsTaskCreatedEvent(taskId = readTask.taskId)
            .usingReferenceId(readTask.referenceId)
            .addToHistory()

        CoordinatorReadStreamsResultEvent(status = TaskStatus.Completed)
            .producedFrom(readTask)
            .addToHistory()

        val projection = TaskProjection(history)
        assertThat(projection.projectStreamReadStatus()).isEqualTo(TaskStatus.Completed)
    }

    @Test
    @DisplayName("""
        Når ReadStreamsTaskCreatedEvent finnes
        Og resultat ikke finnes
        Så:
            Skal projectStreamReadStatus være Pending
    """)
    fun testChainAcknowledgementPending1() {

        val readTask = MediaReadTask(fileUri = "").newReferenceId()

        CoordinatorReadStreamsTaskCreatedEvent(taskId = readTask.taskId)
            .usingReferenceId(readTask.referenceId)
            .addToHistory()

        val projection = TaskProjection(history)
        assertThat(projection.projectStreamReadStatus()).isEqualTo(TaskStatus.Pending)
    }

    @Test
    @DisplayName("""
        Når ReadStreamsTaskCreatedEvent finnes
        Og resultat er Failed
        Så:
            Skal projectStreamReadStatus være Failed
    """)
    fun testChainAcknowledgementFailure1() {

        val readTask = MediaReadTask(fileUri = "").newReferenceId()

        CoordinatorReadStreamsTaskCreatedEvent(taskId = readTask.taskId)
            .usingReferenceId(readTask.referenceId)
            .addToHistory()

        CoordinatorReadStreamsResultEvent(status = TaskStatus.Failed)
            .producedFrom(readTask)
            .addToHistory()


        val projection = TaskProjection(history)
        assertThat(projection.projectStreamReadStatus()).isEqualTo(TaskStatus.Failed)
    }

    @Test
    @DisplayName("""
        Når ConvertSubtitles er eneste operasjon
        Hvis ingen ConvertTaskCreatedEvent finnes
        Så:
            Skal status være NotInitiated
    """)
    fun standalone_notInitiated() {
        StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ConvertSubtitles),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        ).newReferenceId().addToHistory()

        val projection = TaskProjection(history)
        assertThat(projection.projectConvertStatus()).isEqualTo(TaskStatus.NotInitiated)
    }

    @Test
    @DisplayName("""
        Når ConvertSubtitles er eneste operasjon
        Hvis ConvertTaskCreatedEvent finnes
        Så:
            Skal status være Pending
    """)
    fun standalone_pending_after_created() {

        StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ConvertSubtitles),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        ).newReferenceId().addToHistory()

        val convert = ConvertTask(
            data = ConvertTask.Data(
                inputFile = "file:///unit/${UUID.randomUUID()}.ass",
                language = "eng",
                outputDirectory = "unit",
                outputFileName = "UnitText",
                formats = emptyList(),
                allowOverwrite = false
            )
        ).derivedOf(history.first())

        ConvertTaskCreatedEvent(taskId = convert.taskId)
            .derivedOf(history.first())
            .addToHistory()

        val projection = TaskProjection(history)
        assertThat(projection.projectConvertStatus()).isEqualTo(TaskStatus.Pending)
    }

    @Test
    @DisplayName("""
        Når ConvertSubtitles er eneste operasjon
        Hvis ConvertTaskResultEvent Completed finnes
        Så:
            Skal status være Completed
    """)
    fun standalone_completed() {

        StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ConvertSubtitles),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        ).newReferenceId().addToHistory()

        val startEvent = history.first()

        val convert = ConvertTask(
            data = ConvertTask.Data(
                inputFile = "file:///unit/${UUID.randomUUID()}.ass",
                language = "eng",
                outputDirectory = "unit",
                outputFileName = "UnitText",
                formats = emptyList(),
                allowOverwrite = false
            )
        ).derivedOf(startEvent)

        ConvertTaskCreatedEvent(taskId = convert.taskId)
            .usingReferenceId(convert.referenceId)
            .addToHistory()

        ConvertTaskResultEvent(
            data = ConvertTaskResultEvent.ConvertedData(
                language = "eng",
                baseName = "UnitText",
                outputFiles = emptyList(),
            ),
            status = TaskStatus.Completed
        )
            .producedFrom(convert)
            .addToHistory()

        val projection = TaskProjection(history)
        assertThat(projection.projectConvertStatus()).isEqualTo(TaskStatus.Completed)
    }

    // ---------------------------------------------------------
    //  PIPELINE MODE (DISSE VAR FEIL – NÅ FIKSET)
    // ---------------------------------------------------------

    @Test
    @DisplayName("""
        Når ExtractSubtitles og ConvertSubtitles er planlagt
        Hvis Extract ikke har startet
        Så:
            Skal status være NotInitiated
    """)
    fun pipeline_notInitiated_before_extract() {
        history.clear()

        StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ExtractSubtitles, OperationType.ConvertSubtitles),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        ).newReferenceId().addToHistory()

        val projection = TaskProjection(history)
        assertThat(projection.projectConvertStatus()).isEqualTo(TaskStatus.NotInitiated)
    }

    @Test
    @DisplayName("""
        Når ExtractSubtitles og ConvertSubtitles er planlagt
        Hvis både ExtractSubtitleTaskCreatedEvent og ConvertTaskCreatedEvent finnes
        Så:
            Skal status fortsatt være Pending inntil resultat foreligger
    """)
    fun pipeline_pending_until_convert_result() {
        history.clear()

        val start = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ExtractSubtitles, OperationType.ConvertSubtitles),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        ).newReferenceId().addToHistory()

        val extractSourceFile = "file:///unit/${UUID.randomUUID()}.mkv"
        val extract = ExtractSubtitleTask(
            data = ExtractSubtitleData(
                inputFile = extractSourceFile,
                outputFolderName = "unit",
                outputFileName = "unit",
                language = "eng",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply {  },
                    output = OutputSection(extractSourceFile).apply {  },
                )
            )
        ).derivedOf(start)

        val convert = ConvertTask(
            data = ConvertTask.Data(
                inputFile = "file:///unit/${UUID.randomUUID()}.ass",
                language = "eng",
                outputDirectory = "unit",
                outputFileName = "UnitText",
                formats = emptyList(),
                allowOverwrite = false
            )
        ).derivedOf(start)

        ProcesserExtractTaskCreatedEvent(taskIds = setOf(MultiTaskIdentity(extract.taskId, "Potet")))
            .derivedOf(start)
            .addToHistory()

        ConvertTaskCreatedEvent(taskId = convert.taskId)
            .derivedOf(start)
            .addToHistory()

        val projection = TaskProjection(history)
        assertThat(projection.projectConvertStatus()).isEqualTo(TaskStatus.Pending)
    }

    @Test
    @DisplayName("""
        Når ExtractSubtitles og ConvertSubtitles er planlagt
        Hvis ConvertTaskResultEvent Completed finnes
        Så:
            Skal status være Completed
    """)
    fun pipeline_completed() {
        history.clear()

        val start = StartProcessingEvent(
            data = StartData(
                operation = setOf(OperationType.ExtractSubtitles, OperationType.ConvertSubtitles),
                fileUri = "file:///unit/${UUID.randomUUID()}.mkv"
            )
        ).newReferenceId().addToHistory()

        val extractSourceFile = "file:///unit/${UUID.randomUUID()}.mkv"
        val extract = ExtractSubtitleTask(
            data = ExtractSubtitleData(
                inputFile = extractSourceFile,
                outputFolderName = "unit",
                outputFileName = "unit",
                language = "eng",
                instructions = FFmpegInstructions(
                    inputs = InputSection().apply {  },
                    output = OutputSection(extractSourceFile).apply {  },
                )
            )
        ).derivedOf(start)

        val convert = ConvertTask(
            data = ConvertTask.Data(
                inputFile = "file:///unit/${UUID.randomUUID()}.ass",
                language = "eng",
                outputDirectory = "unit",
                outputFileName = "UnitText",
                formats = emptyList(),
                allowOverwrite = false
            )
        ).derivedOf(start)

        ProcesserExtractTaskCreatedEvent(taskIds = setOf(MultiTaskIdentity(extract.taskId, "Potet")))
            .derivedOf(start)
            .addToHistory()

        ConvertTaskCreatedEvent(taskId = convert.taskId)
            .derivedOf(start)
            .addToHistory()

        ConvertTaskResultEvent(
            data = ConvertTaskResultEvent.ConvertedData(
                language = "eng",
                baseName = "UnitText",
                outputFiles = emptyList()
            ),
            status = TaskStatus.Completed
        )
            .producedFrom(convert)
            .addToHistory()

        val projection = TaskProjection(history)
        assertThat(projection.projectConvertStatus()).isEqualTo(TaskStatus.Completed)
    }

}
