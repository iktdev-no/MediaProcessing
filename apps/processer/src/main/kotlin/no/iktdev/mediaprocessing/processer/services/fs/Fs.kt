package no.iktdev.mediaprocessing.processer.services.fs

import java.io.File

class Fs: IFs {
    override fun readText(path: String) =
        try { File(path).readText() } catch (_: Exception) { null }

    override fun readLines(path: String) =
        try { File(path).readLines() } catch (_: Exception) { null }

    override fun list(path: String) =
        try { File(path).list()?.toList() } catch (_: Exception) { null }

    override fun writeText(path: String, content: String) =
        try { File(path).writeText(content); true } catch (_: Exception) { false }

    override fun appendText(path: String, content: String) =
        try { File(path).appendText(content); true } catch (_: Exception) { false }

    override fun exists(path: String) =
        File(path).exists()

    override fun mkdirs(path: String) =
        File(path).mkdirs()

    override fun deleteRecursively(path: String) =
        File(path).deleteRecursively()

    override fun canWrite(path: String): Boolean {
        return File(path).canWrite()
    }
}