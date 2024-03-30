package no.iktdev.mediaprocessing

import kotlinx.serialization.json.*
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.persistance.events
import no.iktdev.mediaprocessing.shared.kafka.core.DeserializingRegistry
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import org.json.JSONArray
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class PersistentMessageFromJsonDump(events: String) {
    private var data: JsonArray?

    init {
        val jsonArray = Json.parseToJsonElement(events) as JsonArray
        data = jsonArray.firstOrNull { it.jsonObject["data"] != null }?.jsonObject?.get("data") as? JsonArray
    }

    fun getPersistentMessages(): List<PersistentMessage> {
        return data?.mapNotNull {
            try {
                mapToPersistentMessage(it)
            } catch (e: Exception) {
                System.err.print(it.toString())
                e.printStackTrace()
                null
            }
        } ?: emptyList()
    }

    val dzz = DeserializingRegistry()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
    private fun mapToPersistentMessage(e: JsonElement): PersistentMessage? {
        val referenceId: String = e.jsonObject["referenceId"]?.jsonPrimitive?.content ?: throw RuntimeException("No ReferenceId found")
        val eventId: String = e.jsonObject["eventId"]?.jsonPrimitive?.content ?: throw RuntimeException("No EventId")
        val event: String = e.jsonObject["event"]?.jsonPrimitive?.content ?: throw RuntimeException("No Event")
        val data: String = e.jsonObject["data"]?.jsonPrimitive?.content ?: throw RuntimeException("No data")
        val created: String = e.jsonObject["created"]?.jsonPrimitive?.content ?: throw RuntimeException("No Created date time found")

        val kev = KafkaEvents.toEvent(event) ?: throw RuntimeException("Not able to convert event to Enum")
        val dzdata = dzz.deserializeData(kev, data)

        return PersistentMessage(
            referenceId = referenceId,
            eventId = eventId,
            event = kev,
            data = dzdata,
            created = LocalDateTime.parse(created, formatter)
        )

    }


}