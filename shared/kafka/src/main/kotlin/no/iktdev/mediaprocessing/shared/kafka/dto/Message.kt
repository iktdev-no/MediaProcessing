package no.iktdev.mediaprocessing.shared.kafka.dto

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*

open class Message<C: MessageDataWrapper>(
    override val referenceId: String = UUID.randomUUID().toString(),
    val eventId: String = UUID.randomUUID().toString(),
    val data: C? = null
): CollectionReference() {

    fun dataAsJson(): String = Gson().toJson(this.data)

    fun <C> dataAs(clazz: C): C? {
        return try {
            val typeToken = object : TypeToken<C>() {}.type
            val gson = Gson()
            val json: String = gson.toJson(data)
            gson.fromJson<C>(json, typeToken)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun <C> dataAs(type: Type): C? {
        return try {
            val gson = Gson()
            val json = dataAsJson()
            gson.fromJson<C>(json, type)
        } catch (e: Exception) {
            null
        }
    }
}
