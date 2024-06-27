package no.iktdev.mediaprocessing.converter.convert

import no.iktdev.library.subtitle.Configuration
import no.iktdev.library.subtitle.Syncro
import no.iktdev.library.subtitle.classes.Dialog
import no.iktdev.library.subtitle.classes.DialogType
import no.iktdev.library.subtitle.export.Export
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.library.subtitle.reader.Reader
import no.iktdev.mediaprocessing.converter.ConverterEnv
import no.iktdev.mediaprocessing.shared.common.task.ConvertTaskData
import no.iktdev.mediaprocessing.shared.contract.dto.SubtitleFormats
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkerRequest
import java.io.File
import kotlin.jvm.Throws

class Converter2(val data: ConvertTaskData,
    private val listener: ConvertListener
) {

    @Throws(FileUnavailableException::class)
    private fun getReader(): BaseReader? {
        val file = File(data.inputFile)
        if (!file.canRead())
            throw FileUnavailableException("Can't open file for reading..")
        return Reader(file).getSubtitleReader()
    }

    private fun syncDialogs(input: List<Dialog>): List<Dialog> {
        return if (ConverterEnv.syncDialogs) Syncro().sync(input) else input
    }

    fun canRead(): Boolean {
        try {
            val reader = getReader()
            return reader != null
        } catch (e: FileUnavailableException) {
            return false
        }
    }

    @Throws(FileUnavailableException::class, FileIsNullOrEmpty::class)
    fun execute() {
        val file = File(data.inputFile)
        listener.onStarted(file.absolutePath)
        try {
            Configuration.exportJson = true
            val read = getReader()?.read() ?: throw FileIsNullOrEmpty()
            if (read.isEmpty())
                throw FileIsNullOrEmpty()
            val filtered = read.filter { !it.ignore && it.type !in listOf(DialogType.SIGN_SONG, DialogType.CAPTION) }
            val syncOrNotSync = syncDialogs(filtered)

            val exporter = Export(file, File(data.outDirectory), data.outFileBaseName)

            val outFiles = if (data.outFormats.isEmpty()) {
                exporter.write(syncOrNotSync)
            } else {
                val exported = mutableListOf<File>()
                if (data.outFormats.contains(SubtitleFormats.SRT)) {
                    exported.add(exporter.writeSrt(syncOrNotSync))
                }
                if (data.outFormats.contains(SubtitleFormats.SMI)) {
                    exported.add(exporter.writeSmi(syncOrNotSync))
                }
                if (data.outFormats.contains(SubtitleFormats.VTT)) {
                    exported.add(exporter.writeVtt(syncOrNotSync))
                }
                exported
            }
            listener.onCompleted(file.absolutePath, outFiles.map { it.absolutePath })
        } catch (e: Exception) {
            listener.onError(file.absolutePath, e.message ?: e.localizedMessage)
        }
    }


    class FileIsNullOrEmpty(override val message: String? = "File read is null or empty"): RuntimeException()
    class FileUnavailableException(override val message: String): RuntimeException()
}