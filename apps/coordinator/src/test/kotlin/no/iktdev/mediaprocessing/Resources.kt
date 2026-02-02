package no.iktdev.mediaprocessing

import no.iktdev.eventi.models.Event
import org.json.JSONArray

enum class Files(val fileName: String) {
    MultipleLanguageBased("Events.json"),
    MediaStreamParsedEvent("MediaStreamParsedEvent.json")
}

fun Files.getContent(): String? {
    return this.javaClass.classLoader.getResource(this.fileName)?.readText()
}

fun Files.databaseJsonToEvents(): List<Event> {
    val content = this.getContent();
    try {
        val jarr = JSONArray(content)
        for (i in 0 until jarr.length()) {
            val o = jarr.getJSONObject(i)
            if (o.has("type") && o.getString("type") == "table") {
                val dataArray = o.getJSONArray("data")

                val events: MutableList<Event> = mutableListOf()
                for (x in 0 until dataArray.length()) {

                    val obj = dataArray.getJSONObject(x)
                    val eventType = obj.getString("event")
                    val dataString = obj.getString("data")
//                    dataString.jsonToEvent(eventType).also {
//                        events.add(it)
//                    }
                }
                return events
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}