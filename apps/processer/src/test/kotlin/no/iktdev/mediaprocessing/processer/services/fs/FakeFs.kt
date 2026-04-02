package no.iktdev.mediaprocessing.processer.services.fs

import org.junit.jupiter.api.Assertions.*

class FakeFs : IFs {

    data class WriteEntry(val path: String, val file: String, val content: String)

    val writeLog = mutableListOf<WriteEntry>()

    private val files = mutableMapOf<String, String>()
    private val dirs = mutableSetOf<String>()

    init {
        dirs.add("/")
    }

    private fun normalize(path: String): String =
        path.replace("//", "/").trimEnd('/').ifEmpty { "/" }

    private fun parentOf(path: String): String =
        normalize(path.substringBeforeLast('/', missingDelimiterValue = "/"))

    override fun readText(path: String): String? =
        files[normalize(path)]

    override fun readLines(path: String): List<String>? =
        files[normalize(path)]?.lines()

    override fun list(path: String): List<String>? {
        val p = normalize(path)
        if (!dirs.contains(p)) return null

        val prefix = if (p == "/") "/" else "$p/"
        val children = mutableSetOf<String>()

        dirs.forEach { d ->
            if (d.startsWith(prefix) && d != p) {
                val rest = d.removePrefix(prefix)
                if (!rest.contains("/")) children.add(rest)
            }
        }

        files.keys.forEach { f ->
            if (f.startsWith(prefix)) {
                val rest = f.removePrefix(prefix)
                if (!rest.contains("/")) children.add(rest)
            }
        }

        return children.toList()
    }

    override fun appendText(path: String, content: String): Boolean {
        val p = normalize(path)
        val parent = parentOf(p)

        // Parent directory must exist
        if (!dirs.contains(parent)) return false

        // If file doesn't exist yet, create it as empty
        val existing = files[p] ?: ""

        // Append content
        val newContent = existing + content
        files[p] = newContent

        // Log write
        val fileName = p.substringAfterLast("/")
        writeLog.add(WriteEntry(path = p, file = fileName, content = content))

        return true
    }


    override fun writeText(path: String, content: String): Boolean {
        val p = normalize(path)
        val parent = parentOf(p)

        if (!dirs.contains(parent)) return false

        files[p] = content

        // Log write
        val fileName = p.substringAfterLast("/")
        writeLog.add(WriteEntry(path = p, file = fileName, content = content))

        return true
    }

    override fun exists(path: String): Boolean {
        val p = normalize(path)
        return dirs.contains(p) || files.containsKey(p)
    }

    override fun mkdirs(path: String): Boolean {
        var current = ""
        val parts = normalize(path).split("/").filter { it.isNotEmpty() }

        dirs.add("/")

        for (part in parts) {
            current += "/$part"
            dirs.add(current)
        }

        return true
    }

    override fun deleteRecursively(path: String): Boolean {
        val p = normalize(path)

        files.keys.filter { it.startsWith(p) }.toList().forEach { files.remove(it) }
        dirs.filter { it.startsWith(p) }.toList().forEach { dirs.remove(it) }

        return true
    }

    fun dump(): String {
        val sb = StringBuilder()
        sb.appendLine("Dirs:")
        dirs.sorted().forEach { sb.appendLine("  $it") }
        sb.appendLine("Files:")
        files.toSortedMap().forEach { (k, v) ->
            sb.appendLine("  $k = ${v.replace("\n", "\\n")}")
        }
        sb.appendLine("Writes:")
        writeLog.forEach { w ->
            sb.appendLine("  ${w.path} = ${w.content}")
        }
        return sb.toString()
    }
}
