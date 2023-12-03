package no.iktdev.mediaprocessing.coordinator.reader

import kotlinx.coroutines.launch
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.mediaprocessing.shared.SharedConfig
import no.iktdev.mediaprocessing.shared.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.parsing.FileNameParser
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service
import java.io.File

@Service
class BaseInfoFromFile {
    val io = Coroutines.io()
    val listener = DefaultMessageListener(SharedConfig.kafkaTopic) { event ->
        val message = event.value()
        if (message.data is ProcessStarted) {
            io.launch {
                readFileInfo(message.referenceId, message.data as ProcessStarted)
            }
        }
    }
    val producer = CoordinatorProducer()

    init {
        io.launch {
            listener.listen()
        }
    }

    suspend fun readFileInfo(referenceId: String, started: ProcessStarted) {
        val result = try {
            val fileName = File(started.file).nameWithoutExtension
            val fileNameParser = FileNameParser(fileName)
            BaseInfoPerformed(
                Status.COMPLETED,
                title = fileNameParser.guessDesiredTitle(),
                sanitizedName = fileNameParser.guessDesiredFileName()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            MessageDataWrapper(Status.ERROR, e.message ?: "Unable to obtain proper info from file")
        }
        producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED, result)
    }
}