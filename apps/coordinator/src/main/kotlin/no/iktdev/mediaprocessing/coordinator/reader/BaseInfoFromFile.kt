package no.iktdev.mediaprocessing.coordinator.reader

import kotlinx.coroutines.launch
import no.iktdev.mediaprocessing.shared.common.ProcessingService
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.kafka.CoordinatorProducer
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameParser
import no.iktdev.mediaprocessing.shared.kafka.core.DefaultMessageListener
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.stereotype.Service
import java.io.File

class BaseInfoFromFile(producer: CoordinatorProducer = CoordinatorProducer(), listener: DefaultMessageListener = DefaultMessageListener(
    SharedConfig.kafkaTopic)): ProcessingService(producer, listener) {

    init {
        listener.onMessageReceived = { event ->
            val message = event.value
            if (message.data is ProcessStarted) {
                io.launch {
                    val result = readFileInfo(message.data as ProcessStarted)
                    onResult(message.referenceId, result)
                }
            }
        }
        io.launch {
            listener.listen()
        }
    }

    override fun onResult(referenceId: String, data: MessageDataWrapper) {
        producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED, data)
    }

    fun readFileInfo(started: ProcessStarted): MessageDataWrapper {
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
        return result
    }
}