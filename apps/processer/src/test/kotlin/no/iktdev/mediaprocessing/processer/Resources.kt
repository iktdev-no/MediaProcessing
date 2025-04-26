package no.iktdev.mediaprocessing.processer

import no.iktdev.mediaprocessing.shared.common.contract.Events
import no.iktdev.mediaprocessing.shared.common.contract.data.Event
import no.iktdev.mediaprocessing.shared.common.contract.jsonToEvent
import org.json.JSONArray

enum class Files(val fileName: String) {
    Output1("encodeProgress1.txt")
}


fun Files.getAsList(): List<String> {
    return this.javaClass.classLoader.getResource(this.fileName)?.readText()?.lines() ?: emptyList()
}


fun Files.getContent(): String? {
    return this.javaClass.classLoader.getResource(this.fileName)?.readText()
}
