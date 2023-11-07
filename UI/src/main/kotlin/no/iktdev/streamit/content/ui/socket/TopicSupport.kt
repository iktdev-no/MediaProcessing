package no.iktdev.streamit.content.ui.socket

import com.google.gson.Gson

abstract class TopicSupport {

    fun toJson(item: Any?): String? {
        return if (item != null) Gson().toJson(item) else null
    }
}