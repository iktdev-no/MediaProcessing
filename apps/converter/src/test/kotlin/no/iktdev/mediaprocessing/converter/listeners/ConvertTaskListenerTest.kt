package no.iktdev.mediaprocessing.converter.listeners

import kotlinx.coroutines.test.runTest
import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.Task
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.eventi.tasks.TaskReporter
import no.iktdev.library.subtitle.classes.Dialog
import no.iktdev.library.subtitle.classes.DialogType
import no.iktdev.library.subtitle.classes.Time
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.mediaprocessing.converter.ConverterEnvironment
import no.iktdev.mediaprocessing.converter.Exporter
import no.iktdev.mediaprocessing.converter.MockConverter
import no.iktdev.mediaprocessing.converter.MockConverterEnvironment
import no.iktdev.mediaprocessing.converter.convert.ConvertListener
import no.iktdev.mediaprocessing.converter.convert.Converter
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.common.model.SubtitleFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis

class ConvertTaskListenerTest {

    class ConvertTaskListenerTestImplementation : ConvertTaskListener() {
        fun getJob() = currentJob


        var overrideEnv: ConverterEnvironment = DefaultConverterEnvironment()
        override fun getConverterEnvironment(): ConverterEnvironment {
            return overrideEnv
        }

        var overrideListener: ConvertListener? = null
        override fun getListener(): ConvertListener {
            if (overrideListener != null)
                return overrideListener!!
            else
                return super.getListener()
        }

        var overrideConverter: Converter? = null
        override fun getConverter(): Converter {
            return if (overrideConverter != null)
                overrideConverter!!
            else
                super.getConverter()

        }
    }

    val listener = ConvertTaskListenerTestImplementation()

    // ---------------------------------------------------------------------
    // Fake environment + fake converter
    // ---------------------------------------------------------------------

    class FakeListener : ConvertListener {
        var started = 0
        var completed = 0
        var errors = 0

        override fun onStarted(inputFile: String) {
            started++
        }

        override fun onCompleted(inputFile: String, outputFiles: List<String>) {
            completed++
        }

        override fun onError(inputFile: String, message: String) {
            errors++
        }
    }

    class FakeExporter(
        private val files: List<File>,
        private val shouldThrow: Boolean = false
    ) : Exporter {

        override fun write(dialogs: List<Dialog>): MutableList<File> {
            if (shouldThrow) throw RuntimeException("export failed")
            return files.toMutableList()
        }

        override fun writeSrt(dialogs: List<Dialog>) = files[0]
        override fun writeSmi(dialogs: List<Dialog>) = files[0]
        override fun writeVtt(dialogs: List<Dialog>) = files[0]
    }

    class FakeReader(
        private val dialogs: List<Dialog>,
        private val shouldThrow: Boolean = false
    ) : BaseReader() {
        override fun read(): List<Dialog> {
            if (shouldThrow) throw RuntimeException("reader failed")
            return dialogs
        }
    }


    val overrideReporter = object : TaskReporter {
        override fun markClaimed(taskId: UUID, workerId: String) {}
        override fun updateLastSeen(taskId: UUID) {}
        override fun markCompleted(taskId: UUID) {}
        override fun markFailed(taskId: UUID) {}

        override fun updateProgress(taskId: UUID, progress: Int) {}
        override fun log(taskId: UUID, message: String) {}
        override fun publishEvent(event: Event) {

        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun makeTask(
        formats: List<SubtitleFormat> = emptyList()
    ): ConvertTask {
        return ConvertTask(
            ConvertTask.Data(
                inputFile = "input.srt",
                language = "no",
                outputDirectory = "out",
                outputFileName = "name",
                formats = formats,
                allowOverwrite = true
            )
        ).apply { newReferenceId() }
    }

    // ---------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("""
        Når onTask kjøres og converterer
        Hvis den bruker lengre tid
        Så:
            Skal koden vente til den er ferdig
    """)
    fun onTask_validate_delay() = runTest {
        val delay = 1000L
        val converter = MockConverter(
            delay,
            listOf("file:///potato.srt"),
            listener = FakeListener()
        )
        listener.apply {
            overrideConverter = converter
        }
        val task = makeTask()
        val event = listener.onTask(task)

        val time = measureTimeMillis {
            val accepted = listener.accept(task, overrideReporter)
            assertTrue(accepted, "Task listener did not accept the task.")
            listener.getJob()?.join()
            assertTrue(event is ConvertTaskResultEvent)
            assertEquals(TaskStatus.Completed, (event as ConvertTaskResultEvent).status)
        }
    }


    @Test
    @DisplayName(
        """
        Når onTask kjøres
        Hvis converter lykkes
        Så returneres Completed-event
        """
    )
    fun onTask_success() = runTest {
        val dialogs = listOf(
            Dialog("0", Time(0, 0, 0, 0), Time(0, 0, 1000, 0), "Hello", DialogType.DIALOG_NORMAL)
        )

        val env = MockConverterEnvironment(
            canReadValue = true,
            reader = FakeReader(dialogs),
            exporter = FakeExporter(listOf(File("/fake/out.srt")))
        )

        listener.apply {
            overrideListener = FakeListener()
            overrideEnv = env
        }

        val task = makeTask()

        val event = listener.onTask(task) as ConvertTaskResultEvent

        assertEquals(TaskStatus.Completed, event.status)
        assertEquals(listOf("/fake/out.srt"), event.data!!.outputFiles)
    }

    @Test
    @DisplayName(
        """
        Når onTask kjøres
        Hvis converter feiler
        Så returneres Failed-event
        """
    )
    fun onTask_failure() = runTest {
        val env = MockConverterEnvironment(
            canReadValue = true,
            reader = FakeReader(emptyList()) // triggers FileIsNullOrEmpty
        )

        listener.apply {
            overrideListener = FakeListener()
            overrideEnv = env
        }

        val task = makeTask()

        val event = listener.onTask(task) as ConvertTaskResultEvent

        assertEquals(TaskStatus.Failed, event.status)
        assertNull(event.data)
    }

    @Test
    @DisplayName(
        """
        Når supports() kalles
        Så returnerer den true for ConvertTask og false for andre typer
        """
    )
    fun supports_test() {
        val listener = ConvertTaskListener()

        assertTrue(listener.supports(makeTask()))
        assertFalse(listener.supports(DummyTask()))
    }

    class DummyTask : Task()
}
