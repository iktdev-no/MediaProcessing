package no.iktdev.mediaprocessing.coordinator.reader

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreatorListener
import no.iktdev.mediaprocessing.shared.common.ProcessingService
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.common.runner.CodeToOutput
import no.iktdev.mediaprocessing.shared.common.runner.getOutputUsing
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class ReadVideoFileStreams(@Autowired var coordinator: Coordinator): ProcessingService() {

    override fun onResult(referenceId: String, data: MessageDataWrapper) {
        producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED, data)
    }

    override fun onReady() {
        coordinator.addListener(object : TaskCreatorListener {
            override fun onEventReceived(referenceId: String, event: PersistentMessage, events: List<PersistentMessage>) {
                if (event.event == KafkaEvents.EVENT_PROCESS_STARTED && event.data.isSuccess()) {
                    io.launch {
                        val result = fileReadStreams(event.data as ProcessStarted)
                        onResult(referenceId, result)
                    }
                }
            }

        })
    }

    suspend fun fileReadStreams(started: ProcessStarted): MessageDataWrapper {
        val file = File(started.file)
        return if (file.exists() && file.isFile) {
            val result = readStreams(file)
            val joined = result.output.joinToString(" ")
            val jsoned = Gson().fromJson(joined, JsonObject::class.java)
            ReaderPerformed(Status.COMPLETED, file = started.file, output = jsoned)
        } else {
            MessageDataWrapper(Status.ERROR, "File in data is not a file or does not exist")
        }
    }

    suspend fun readStreams(file: File): CodeToOutput {
        val result = getOutputUsing(
            SharedConfig.ffprobe,
            "-v", "quiet", "-print_format", "json", "-show_streams", file.absolutePath
        )
        return result
    }

}