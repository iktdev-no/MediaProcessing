package no.iktdev.mediaprocessing.processer

import no.iktdev.exfl.using
import java.io.File

object Util {
    fun getTemporaryStoreFile(fileName: String): File {
        return ProcesserEnv.cachedContent.using(fileName)
    }
}