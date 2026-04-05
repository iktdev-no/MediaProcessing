package no.iktdev.mediaprocessing.processer.services.fs

interface IFs {
    fun readText(path: String): String?
    fun readLines(path: String): List<String>?
    fun list(path: String): List<String>?

    fun canWrite(path: String): Boolean
    fun appendText(path: String, content: String): Boolean
    fun writeText(path: String, content: String): Boolean
    fun exists(path: String): Boolean
    fun mkdirs(path: String): Boolean
    fun deleteRecursively(path: String): Boolean
}