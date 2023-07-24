package no.iktdev.streamit.content.convert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.iktdev.library.subtitle.Syncro
import no.iktdev.library.subtitle.export.Export
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.library.subtitle.reader.Reader
import no.iktdev.streamit.content.common.dto.reader.SubtitleInfo
import no.iktdev.streamit.content.common.dto.reader.work.ConvertWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import java.io.File

class ConvertRunner(val referenceId: String, val listener: IConvertListener) {

    private fun getReade(inputFile: File): BaseReader? {
        return Reader(inputFile).getSubtitleReader()
    }

    suspend fun readAndConvert (subtitleInfo: SubtitleInfo) {
        val inFile = File(subtitleInfo.inputFile)
        val reader = getReade(inFile)
        val dialogs = reader?.read()
        if (dialogs.isNullOrEmpty()) {
            withContext(Dispatchers.Default) {
                listener.onError(referenceId, subtitleInfo, "Dialogs read from file is null or empty!")
            }
            return
        }

        withContext(Dispatchers.Default) {
            listener.onStarted(referenceId)
        }

        val syncedDialogs = Syncro().sync(dialogs)

        try {
            val converted = Export(inFile, syncedDialogs).write()
            converted.forEach {
                val item = ConvertWork(
                    inFile = inFile.absolutePath,
                    collection = subtitleInfo.collection,
                    language = subtitleInfo.language,
                    outFile = it.absolutePath
                )
                withContext(Dispatchers.Default) {
                    listener.onEnded(referenceId, subtitleInfo, work = item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Default) {
                listener.onError(referenceId, subtitleInfo, "See log")
            }
        }

    }


}

interface IConvertListener {
    fun onStarted(referenceId: String)
    fun onError(referenceId: String, info: SubtitleInfo, message: String)
    fun onEnded(referenceId: String, info: SubtitleInfo, work: ConvertWork)
}