package no.iktdev.mediaprocessing.converter.convert

import kotlinx.coroutines.test.runTest
import no.iktdev.files.IFile
import no.iktdev.library.subtitle.classes.Dialog
import no.iktdev.library.subtitle.classes.DialogType
import no.iktdev.library.subtitle.classes.Time
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.mediaprocessing.converter.ConverterEnvironment
import no.iktdev.mediaprocessing.converter.Exporter
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.common.model.SubtitleFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class Converter2Test {

    // -------------------------------------------------------------------------
    // Fake implementations
    // -------------------------------------------------------------------------

    class FakeReader(
        private val dialogs: List<Dialog>,
        private val shouldThrow: Boolean = false
    ) : BaseReader() {
        override fun read(): List<Dialog> {
            if (shouldThrow) throw RuntimeException("reader failed")
            return dialogs
        }
    }

    class FakeExporter(
        private val srtFile: IFile? = null,
        private val smiFile: IFile? = null,
        private val vttFile: IFile? = null,
        private val filesForWrite: List<IFile> = emptyList(),
        private val shouldThrow: Boolean = false
    ) : Exporter {

        override fun write(dialogs: List<Dialog>): MutableList<IFile> {
            if (shouldThrow) throw RuntimeException("export failed")
            return filesForWrite.toMutableList()
        }

        override fun writeSrt(dialogs: List<Dialog>): IFile =
            srtFile ?: error("srtFile not set in FakeExporter")

        override fun writeSmi(dialogs: List<Dialog>): IFile =
            smiFile ?: error("smiFile not set in FakeExporter")

        override fun writeVtt(dialogs: List<Dialog>): IFile =
            vttFile ?: error("vttFile not set in FakeExporter")
    }


    class FakeListener : ConvertListener {
        var started = 0
        var completed = 0
        var errors = 0
        var lastError: String? = null

        override fun onStarted(inputFile: String) {
            started++
        }

        override fun onCompleted(inputFile: String, outputFiles: List<String>) {
            completed++
        }

        override fun onError(inputFile: String, message: String) {
            errors++
            lastError = message
        }
    }

    class FakeEnv(
        var canReadValue: Boolean = true,
        var reader: BaseReader? = null,
        var exporter: Exporter? = null
    ) : ConverterEnvironment {

        override fun canRead(file: IFile): Boolean = canReadValue

        override fun getReader(file: IFile): BaseReader? = reader

        override fun createExporter(input: IFile, outputDir: IFile, name: String): Exporter {
            return exporter ?: error("FakeEnv.exporter must be set before calling createExporter")
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun makeTaskData(
        input: String = "input.srt",
        language: String = "no",
        outDir: String = "out",
        outName: String = "name",
        formats: List<SubtitleFormat> = emptyList(),
        allowOverwrite: Boolean = true
    ) = ConvertTask.Data(
        inputFile = input,
        language = language,
        outputDirectory = outDir,
        outputFileName = outName,
        formats = formats,
        allowOverwrite = allowOverwrite
    )

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------


    @Test
    @DisplayName(
        """
        Når execute() kjøres
        Hvis reader returnerer tom liste
        Så kalles onError
        """
    )
    fun execute_emptyFile() = runTest {
        val env = FakeEnv(
            canReadValue = true,
            reader = FakeReader(emptyList())
        )
        val listener = FakeListener()

        val converter = Converter2(
            env = env,
            listener = listener
        )

        converter.convert(makeTaskData())

        assertEquals(1, listener.errors)
        assertEquals(0, listener.completed)
    }

    @Test
    @DisplayName(
        """
        Når execute() kjøres
        Hvis reader returnerer dialoger
        Og exporter.write lykkes
        Så kalles onCompleted
        """
    )
    fun execute_success() = runTest  {
        val dialogs = listOf(
            Dialog("0", Time(0, 0, 0, 0), Time(0, 0, 1000, 0), "Hello", DialogType.DIALOG_NORMAL)
        )

        val env = FakeEnv(
            canReadValue = true,
            reader = FakeReader(dialogs),
            exporter = FakeExporter(srtFile = IFile("/fake/out.srt"))
        )

        val listener = FakeListener()

        val converter = Converter2(
            env = env,
            listener = listener
        )

        converter.convert(
            makeTaskData(
                formats = listOf(SubtitleFormat.SRT)
            )
        )

        assertEquals(1, listener.completed)
        assertEquals(listOf("/fake/out.srt"), converter.getResult())
    }

    @Test
    @DisplayName(
        """
        Når execute() kjøres
        Hvis exporter.write kaster exception
        Så kalles onError
        """
    )
    fun execute_exportFails() = runTest  {
        val dialogs = listOf(
            Dialog("0", Time(0, 0, 0, 0), Time(0, 0, 1000, 0), "Hello", DialogType.DIALOG_NORMAL)
        )

        val env = FakeEnv(
            canReadValue = true,
            reader = FakeReader(dialogs),
            exporter = FakeExporter(srtFile = IFile("/fake/out.srt"), shouldThrow = true)
        )

        val listener = FakeListener()

        val converter = Converter2(
            env = env,
            listener = listener
        )

        converter.convert(makeTaskData())

        assertEquals(1, listener.errors)
        assertEquals(0, listener.completed)
    }

    @Test
    @DisplayName(
        """
        Når execute() kjøres
        Hvis formats inneholder SRT og VTT
        Så brukes writeSrt og writeVtt
        """
    )
    fun execute_multipleFormats() = runTest  {
        val dialogs = listOf(
            Dialog("0", Time(0, 0, 0, 0), Time(0, 0, 1000, 0), "Hello", DialogType.DIALOG_NORMAL)
        )


        val env = FakeEnv(
            canReadValue = true,
            reader = FakeReader(dialogs),
            exporter = FakeExporter(srtFile = IFile("/fake/out.srt"), vttFile = IFile("/fake/out.vtt"))
        )

        val listener = FakeListener()

        val converter = Converter2(
            env = env,
            listener = listener
        )

        converter.convert(
            makeTaskData(
                formats = listOf(SubtitleFormat.SRT, SubtitleFormat.VTT)
            )
        )

        assertEquals(1, listener.completed)
        assertEquals(listOf("/fake/out.srt", "/fake/out.vtt"), converter.getResult())
    }
}

