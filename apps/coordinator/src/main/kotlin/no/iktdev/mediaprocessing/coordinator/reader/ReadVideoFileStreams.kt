package no.iktdev.mediaprocessing.coordinator.reader

import kotlinx.coroutines.launch
import no.iktdev.mediaprocessing.shared.common.ProcessingService
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.common.runner.CodeToOutput
import no.iktdev.mediaprocessing.shared.common.runner.getOutputUsing
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ReaderPerformed
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service
import java.io.File

class ReadVideoFileStreams(producer: CoordinatorProducer = CoordinatorProducer(), listener: DefaultMessageListener = DefaultMessageListener(
    SharedConfig.kafkaTopic)
): ProcessingService(producer, listener) {

    override fun onResult(referenceId: String, data: MessageDataWrapper) {
        producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_READ_STREAM_PERFORMED, data)
    }

    init {
        listener.onMessageReceived = { event ->
            val message = event.value
            if (message.data is ProcessStarted) {
                io.launch {
                    val result = fileReadStreams(message.data as ProcessStarted)
                    onResult(message.referenceId, result)
                }
            }
        }
        io.launch {
            listener.listen()
        }
    }

    suspend fun fileReadStreams(started: ProcessStarted): MessageDataWrapper {
        val file = File(started.file)
        return if (file.exists() && file.isFile) {
            val result = readStreams(file)
            ReaderPerformed(Status.COMPLETED, file = started.file, output = result.output.joinToString("\n"))
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