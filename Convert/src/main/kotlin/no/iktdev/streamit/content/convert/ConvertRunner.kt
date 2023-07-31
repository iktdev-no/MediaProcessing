package no.iktdev.streamit.content.convert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import no.iktdev.library.subtitle.Syncro
import no.iktdev.library.subtitle.export.Export
import no.iktdev.library.subtitle.reader.BaseReader
import no.iktdev.library.subtitle.reader.Reader
import no.iktdev.streamit.content.common.dto.reader.SubtitleInfo
import no.iktdev.streamit.content.common.dto.reader.work.ConvertWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import java.io.File

private val logger = KotlinLogging.logger {}


class ConvertRunner(val referenceId: String, val listener: IConvertListener) {

    private fun getReade(inputFile: File): BaseReader? {
        return Reader(inputFile).getSubtitleReader()
    }
    private val maxDelay = 1000 * 5
    private var currentDelayed = 0
    suspend fun readAndConvert (subtitleInfo: SubtitleInfo) {
        val inFile = File(subtitleInfo.inputFile)
        while (!inFile.canRead()) {
            if (currentDelayed > maxDelay) {
                logger.error { "Could not out wait lock on file!" }
                withContext(Dispatchers.Default) {
                    listener.onError(referenceId, subtitleInfo, "Cant read file!")
                }
                return
            }
            logger.error { "$referenceId ${subtitleInfo.inputFile}: Cant read file!" }
            delay(500)
            currentDelayed += 500
        }
        val reader = getReade(inFile)
        val dialogs = reader?.read()
        if (dialogs.isNullOrEmpty()) {
            logger.error { "$referenceId ${subtitleInfo.inputFile}: Dialogs read from file is null or empty!" }
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