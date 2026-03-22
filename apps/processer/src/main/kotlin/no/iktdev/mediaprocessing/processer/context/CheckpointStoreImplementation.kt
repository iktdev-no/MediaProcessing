package no.iktdev.mediaprocessing.processer.context

import com.google.gson.Gson
import no.iktdev.files.IFile

class CheckpointStore(
    private val file: IFile
) {

    data class Checkpoint(
        val completed: MutableSet<Int> = mutableSetOf()
    )

    fun load(): Checkpoint {
        if (!file.exists()) return Checkpoint()

        val text = file.readText().trim()
        if (text.isEmpty()) return Checkpoint()

        return try {
            Gson().fromJson(text, Checkpoint::class.java) ?: Checkpoint()
        } catch (e: Exception) {
            Checkpoint()
        }
    }


    fun save(cp: Checkpoint) {
        file.writeText(Gson().toJson(cp))
    }

    fun markCompleted(index: Int) {
        val cp = load()
        cp.completed += index
        save(cp)
    }
}
