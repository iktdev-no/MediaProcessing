package no.iktdev.mediaprocessing.converter.convert

import no.iktdev.library.subtitle.Configuration
import no.iktdev.library.subtitle.Syncro
import no.iktdev.library.subtitle.classes.Dialog
import no.iktdev.library.subtitle.classes.DialogType
import no.iktdev.library.subtitle.export.Export
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.library.subtitle.reader.Reader
import no.iktdev.mediaprocessing.converter.ConverterEnv
import no.iktdev.mediaprocessing.shared.contract.dto.SubtitleFormats
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ConvertWorkerRequest
import java.io.File
import kotlin.jvm.Throws

class Converter(val referenceId: String, val eventId: String, val data: ConvertWorkerRequest) {

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
    fun execute(): List<File> {
        val file = File(data.inputFile)
        Configuration.exportJson = true
        val read = getReader()?.read() ?: throw FileIsNullOrEmpty()
        if (read.isEmpty())
            throw FileIsNullOrEmpty()
        val filtered = read.filter { !it.ignore && it.type !in listOf(DialogType.SIGN_SONG, DialogType.CAPTION) }
        val syncOrNotSync = syncDialogs(filtered)

        val exporter = Export(file, File(data.outDirectory), data.outFileBaseName)

        return if (data.outFormats.isEmpty()) {
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
    }


    class FileIsNullOrEmpty(override val message: String? = "File read is null or empty"): RuntimeException()
    class FileUnavailableException(override val message: String): RuntimeException()
}