package no.iktdev.eventi.core

import com.google.gson.GsonBuilder
import java.time.LocalDateTime

object WGson {
    val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
        .create()
}