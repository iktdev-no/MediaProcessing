package no.iktdev.mediaprocessing.converter.convert

import no.iktdev.files.IFile
import no.iktdev.library.subtitle.Configuration
import no.iktdev.library.subtitle.Syncro
import no.iktdev.library.subtitle.classes.Dialog
import no.iktdev.library.subtitle.classes.DialogType
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.mediaprocessing.converter.ConverterEnv
import no.iktdev.mediaprocessing.converter.ConverterEnvironment
import no.iktdev.mediaprocessing.shared.common.event_task_contract.tasks.ConvertTask
import no.iktdev.mediaprocessing.shared.common.model.SubtitleFormat

class Converter2(
    env: ConverterEnvironment,
    listener: ConvertListener
): Converter(env = env, listener = listener) {

    @Throws(FileUnavailableException::class)
    override fun getSubtitleReader(useFile: IFile): BaseReader? {
        if (!env.canRead(useFile)) {
            throw FileUnavailableException("Can't open file for reading..")
        }
        return env.getReader(useFile)
    }

    private fun syncDialogs(input: List<Dialog>): List<Dialog> {
        return if (ConverterEnv.syncDialogs) Syncro().sync(input) else input
    }

    @Throws(FileUnavailableException::class, FileIsNullOrEmpty::class)
    override suspend fun convert(data: ConvertTask.Data) {
        val file = IFile(data.inputFile)
        listener.onStarted(file.absolutePath)
        try {
            Configuration.exportJson = true
            val read = getSubtitleReader(file)?.read() ?: throw FileIsNullOrEmpty()
            if (read.isEmpty())
                throw FileIsNullOrEmpty()
            val filtered = read.filter { !it.ignore && it.type !in listOf(DialogType.SIGN_SONG, DialogType.CAPTION) }
            val syncOrNotSync = syncDialogs(filtered)

            val exporter = env.createExporter(file, IFile(data.outputDirectory), data.outputFileName)

            val outFiles = if (data.formats.isEmpty()) {
                exporter.write(syncOrNotSync)
            } else {
                val exported = mutableListOf<IFile>()
                if (data.formats.contains(SubtitleFormat.SRT)) {
                    exported.add(exporter.writeSrt(syncOrNotSync))
                }
                if (data.formats.contains(SubtitleFormat.SMI)) {
                    exported.add(exporter.writeSmi(syncOrNotSync))
                }
                if (data.formats.contains(SubtitleFormat.VTT)) {
                    exported.add(exporter.writeVtt(syncOrNotSync))
                }
                exported
            }
            writtenUris = outFiles.map { it.absolutePath }
            listener.onCompleted(file.absolutePath, writtenUris!!)
        } catch (e: Exception) {
            listener.onError(file.absolutePath, e.message ?: e.localizedMessage)
        }
    }

    override fun getResult(): List<String> {
        if (writtenUris == null) {
            throw IllegalStateException("Execute must be called before getting result")
        }
        return writtenUris!!
    }
}