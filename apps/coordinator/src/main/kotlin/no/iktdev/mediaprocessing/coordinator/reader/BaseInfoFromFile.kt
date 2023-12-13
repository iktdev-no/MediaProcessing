package no.iktdev.mediaprocessing.coordinator.reader

import kotlinx.coroutines.launch
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreatorListener
import no.iktdev.mediaprocessing.shared.common.ProcessingService
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameParser
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.ProcessStarted
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import no.iktdev.streamit.library.kafka.dto.Status
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BaseInfoFromFile(@Autowired var coordinator: Coordinator): ProcessingService() {


    override fun onResult(referenceId: String, data: MessageDataWrapper) {
        producer.sendMessage(referenceId, KafkaEvents.EVENT_MEDIA_READ_BASE_INFO_PERFORMED, data)
    }

    override fun onReady() {
        coordinator.addListener(object : TaskCreatorListener {
            override fun onEventReceived(referenceId: String, event: PersistentMessage, events: List<PersistentMessage>) {
               if (event.event == KafkaEvents.EVENT_PROCESS_STARTED && event.data.isSuccess()) {
                   io.launch {
                       val result = readFileInfo(event.data as ProcessStarted)
                       onResult(referenceId, result)
                   }
               }
            }

        })
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