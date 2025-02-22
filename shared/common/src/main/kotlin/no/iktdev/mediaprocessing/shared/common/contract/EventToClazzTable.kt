package no.iktdev.mediaprocessing.shared.common.contract

import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import no.iktdev.eventi.core.WGson
import no.iktdev.mediaprocessing.shared.common.contract.data.*

private val log = KotlinLogging.logger {}

fun String.fromJsonWithDeserializer(event: Events): Event {
    val clazz = event.dataClass
    clazz?.let { eventClass ->
        try {
            val type = TypeToken.getParameterized(eventClass).type
            return WGson.gson.fromJson<Event>(this, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    try {
        // Fallback
        val type = object : TypeToken<Event>() {}.type
        return WGson.gson.fromJson(this, type)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Default
    val type = object : TypeToken<Event>() {}.type
    log.error { "Failed to convert event: $event and data: $this to proper type!" }
    return WGson.gson.fromJson<Event>(this, type)

}