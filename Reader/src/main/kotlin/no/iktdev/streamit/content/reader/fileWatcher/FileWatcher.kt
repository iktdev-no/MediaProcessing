package no.iktdev.streamit.content.reader.fileWatcher

import com.google.gson.Gson
import dev.vishna.watchservice.KWatchEvent
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.iktdev.exfl.coroutines.Coroutines
import no.iktdev.streamit.content.common.CommonConfig
import no.iktdev.streamit.content.common.Naming

import no.iktdev.streamit.content.reader.ReaderEnv
import no.iktdev.streamit.library.kafka.KnownEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.listener.EventMessageListener
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service
import java.io.File

private val logger = KotlinLogging.logger {}
@Service
class FileWatcher: FileWatcherEvents {
    val messageProducer = DefaultProducer(CommonConfig.kafkaTopic)
    val defaultConsumer = DefaultConsumer(subId = "fileWatcher")

    val queue = FileWatcherQueue()


    val watcherChannel = CommonConfig.incomingContent?.asWatchChannel()
    init {
        Coroutines.io().launch {
            if (watcherChannel == null) {
                logger.error { "Can't start watcherChannel on null!" }
            }
            watcherChannel?.consumeEach {
                when (it.kind) {
                    KWatchEvent.Kind.Deleted -> {
                        queue.removeFromQueue(it.file, this@FileWatcher::onFileRemoved)
                    }
                    KWatchEvent.Kind.Created, KWatchEvent.Kind.Initialized -> {
                        queue.addToQueue(it.file, this@FileWatcher::onFilePending, this@FileWatcher::onFileAvailable)
                    }
                    else -> {
                        logger.info { "Ignoring event kind: ${it.kind.name} for file ${it.file.name}" }
                    }
                }
            }
        }

        object : EventMessageListener(CommonConfig.kafkaTopic, defaultConsumer, listOf(KnownEvents.REQUEST_FILE_READ.event)) {
            override fun onMessage(data: ConsumerRecord<String, Message>) {

                if (data.value().status.statusType == StatusType.SUCCESS) {
                    if (data.value().data is String) {
                        val file = File(CommonConfig.incomingContent, data.value().data as String)
                        Coroutines.io().launch {
                            watcherChannel?.send(KWatchEvent(
                                file = file,
                                kind = KWatchEvent.Kind.Initialized,
                                tag = null
                            ))
                        }
                    }
                }
            }

        }
    }


    override fun onFileAvailable(file: PendingFile) {
        logger.debug { "onFileAvailable har mottatt pendingFile ${file.file.name}" }
        val naming = Naming(file.file.nameWithoutExtension)
        val message = Message(
            referenceId = file.id,
            status = Status(StatusType.SUCCESS),
            data = FileResult(file = file.file.absolutePath, title = naming.guessDesiredTitle(), desiredNewName = naming.guessDesiredFileName())
        )
        logger.debug { "Producing message: ${Gson().toJson(message)}" }
        messageProducer.sendMessage(KnownEvents.EVENT_READER_RECEIVED_FILE.event, message)
    }

    override fun onFilePending(file: PendingFile) {
        val message = Message(
            status = Status(StatusType.PENDING),
            data = FileResult(file = file.file.absolutePath)
        )
        messageProducer.sendMessage(KnownEvents.EVENT_READER_RECEIVED_FILE.event , message)
    }

    override fun onFileFailed(file: PendingFile) {
        val message = Message(
            status = Status(StatusType.ERROR),
            data = file.file.absolutePath
        )
        messageProducer.sendMessage(KnownEvents.EVENT_READER_RECEIVED_FILE.event , message)
    }

    override fun onFileRemoved(file: PendingFile) {
        val message = Message(
            status = Status(StatusType.IGNORED),
            data = file.file.absolutePath
        )
        messageProducer.sendMessage(KnownEvents.EVENT_READER_RECEIVED_FILE.event , message)
    }

    data class FileResult(
        val file: String,
        val title: String = "",
        val desiredNewName: String = ""
    )

}