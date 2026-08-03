package no.iktdev.mediaprocessing.ui.service.sse

import com.google.gson.reflect.TypeToken
import mu.KotlinLogging
import no.iktdev.eventi.serialization.WGson
import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.shared.common.sse.SSEKeys
import no.iktdev.mediaprocessing.ui.LocalProgressCache
import no.iktdev.mediaprocessing.ui.models.translate
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component

@Component
class SSEEventHandler(
    private val sse: SSEServer,
    private val localProgressCache: LocalProgressCache
) {
    private val log = KotlinLogging.logger {}

    fun onEvent(source: String, e: ServerSentEvent<String>) {
        val (event, data) = e.event() to e.data()
        if (event.isNullOrBlank()) run {
            log.error { "Received event without name! Name: ${e.event()}, Data: ${e.data()}, Id: ${e.id()}" }
            return
        }
        println("[$source] Mottok event [$event med data: $data]")
        val key = SSEKeys.fromKey(event) ?: run {
            log.error { "Event [$event] could not be found" }
            return
        }
        if (!data.isNullOrBlank()) {
            processData(key, data)
        }

    }

    fun processData(key: SSEKeys, data: String) {
        when (key) {
            SSEKeys.ProgressRestore -> {
                val type = TypeToken.getParameterized(List::class.java, ProgressUpdate::class.java).type
                val pus: List<ProgressUpdate> = WGson.gson.fromJson(data, type)

                pus.forEach { pu ->
                    handleProgress(pu)
                }
            }
            SSEKeys.Progress -> {
                val pu = WGson.gson.fromJson(data, ProgressUpdate::class.java)
                handleProgress(pu)
            }
            SSEKeys.Ping -> {

            }

            SSEKeys.HealthStatus  -> {
                // Do nothing as we are the sender
            }
        }
    }


    fun handleProgress(pu: ProgressUpdate) {
        val progress = pu.translate()
        localProgressCache.update(progress)
    }
}