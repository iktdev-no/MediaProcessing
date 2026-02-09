package no.iktdev.mediaprocessing.processer.segment

import com.google.gson.Gson
import java.io.File

class SegmentCheckpointStore(private val file: File) {

    data class Checkpoint(val completed: MutableSet<Int> = mutableSetOf())

    fun load(): Checkpoint {
        if (!file.exists()) return Checkpoint()
        return Gson().fromJson(file.readText(), Checkpoint::class.java)
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
