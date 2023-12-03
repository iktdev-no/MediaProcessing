package no.iktdev.mediaprocessing.coordinator.reader

import kotlinx.coroutines.launch
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.SharedConfig
import no.iktdev.mediaprocessing.shared.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.mediaprocessing.shared.runner.CodeToOutput
import no.iktdev.mediaprocessing.shared.runner.getOutputUsing
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service
import java.io.File

@Service
class ReadVideoFileStreams {
    val io = Coroutines.io()
    val listener = DefaultMessageListener(SharedConfig.kafkaTopic) { event ->
        val message = event.value()
        if (message.data is ProcessStarted) {
            io.launch {
                fileReadStreams(message.referenceId, message.data as ProcessStarted)
            }
        }
    }
    val producer = CoordinatorProducer()

    init {
        io.launch {
            listener.listen()
        }
    }

    suspend fun fileReadStreams(referenceId: String, started: ProcessStarted) {
        val file = File(started.file)
        if (file.exists() && file.isFile) {
            val result = readStreams(file)

            producer.sendMessage(
                referenceId, KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED,
                ReaderPerformed(Status.COMPLETED, file = started.file,  output = result.output.joinToString("\n"))
            )
        } else {
            producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED,
                MessageDataWrapper(Status.ERROR, "File in data is not a file or does not exist")
            )
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