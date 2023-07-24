package no.iktdev.streamit.content.convert.kafka

import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.DefaultKafkaReader
import no.iktdev.streamit.content.common.dto.reader.work.ConvertWork
import no.iktdev.streamit.content.common.dto.reader.work.ExtractWork
import no.iktdev.streamit.content.convert.ConvertRunner
import no.iktdev.streamit.content.convert.IConvertListener
import no.iktdev.streamit.content.convert.SubtitleInfo
import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.listener.SimpleMessageListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}

@Service
class SubtitleConsumer: DefaultKafkaReader("convertHandlerSubtitle"), IConvertListener {

    private final val listener = object : SimpleMessageListener(
        topic = CommonConfig.kafkaTopic,
        consumer = defaultConsumer,
        accepts = listOf(KafkaEvents.EVENT_ENCODER_ENDED_SUBTITLE_FILE.event)
    ) {
        override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
            val referenceId = data.value().referenceId
            val workResult = data.value().dataAs(ExtractWork::class.java)

            if (workResult?.produceConvertEvent == true) {
                val convertWork = SubtitleInfo(
                    inputFile = File(workResult.outFile),
                    collection = workResult.collection,
                    language = workResult.language,
                )
                Coroutines.io().launch {
                    ConvertRunner(referenceId, this@SubtitleConsumer).readAndConvert(convertWork)
                }
            } else {
                logger.info { "Skipping ${data.value().referenceId} ${workResult?.outFile} as it is not a convert candidate" }
            }
        }
    }

    init {
        listener.listen()
    }

    override fun onStarted(referenceId: String, info: SubtitleInfo) {
        produceMessage(KafkaEvents.EVENT_CONVERTER_STARTED_SUBTITLE_FILE, Message(referenceId = referenceId, Status(statusType = StatusType.SUCCESS)), info)
    }

    override fun onError(referenceId: String, info: SubtitleInfo, message: String) {
        produceMessage(KafkaEvents.EVENT_CONVERTER_ENDED_SUBTITLE_FILE, Message(referenceId = referenceId, Status(statusType = StatusType.ERROR)), null)
    }

    override fun onEnded(referenceId: String, info: SubtitleInfo, work: ConvertWork) {
        produceMessage(KafkaEvents.EVENT_CONVERTER_ENDED_SUBTITLE_FILE, Message(referenceId = referenceId, Status(statusType = StatusType.SUCCESS)), work)
    }

}