package no.iktdev.streamit.content.convert

import no.iktdev.library.subtitle.Syncro
import no.iktdev.library.subtitle.export.Export
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.library.subtitle.reader.Reader
import no.iktdev.streamit.content.common.dto.reader.work.ConvertWork
import java.io.File

class ConvertRunner(val referenceId: String, val listener: IConvertListener) {

    private fun getReade(inputFile: File): BaseReader? {
        return Reader(inputFile).getSubtitleReader()
    }

    suspend fun readAndConvert (subtitleInfo: SubtitleInfo) {
        val reader = getReade(subtitleInfo.inputFile)
        val dialogs = reader?.read()
        if (dialogs.isNullOrEmpty()) {
            listener.onError(referenceId, subtitleInfo, "Dialogs read from file is null or empty!")
            return
        }
        listener.onStarted(referenceId, subtitleInfo)

        val syncedDialogs = Syncro().sync(dialogs)

        val converted =  Export(subtitleInfo.inputFile, syncedDialogs).write()
         converted.forEach {
             val item = ConvertWork(
                inFile = subtitleInfo.inputFile.absolutePath,
                collection = subtitleInfo.collection,
                language = subtitleInfo.language,
                outFile = it.absolutePath
            )
             listener.onEnded(referenceId, subtitleInfo, work = item)
        }

    }


}

interface IConvertListener {
    fun onStarted(referenceId: String, info: SubtitleInfo)
    fun onError(referenceId: String, info: SubtitleInfo, message: String)
    fun onEnded(referenceId: String, info: SubtitleInfo, work: ConvertWork)
}