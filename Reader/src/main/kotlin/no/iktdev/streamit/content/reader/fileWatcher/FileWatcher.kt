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
import no.iktdev.streamit.content.common.dto.reader.FileResult

import no.iktdev.streamit.library.kafka.KafkaEvents
import no.iktdev.streamit.library.kafka.dto.Message
import no.iktdev.streamit.library.kafka.dto.Status
import no.iktdev.streamit.library.kafka.dto.StatusType
import no.iktdev.streamit.library.kafka.consumers.DefaultConsumer
import no.iktdev.streamit.library.kafka.listener.SimpleMessageListener
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


    val watcherChannel = CommonConfig.incomingContent.asWatchChannel()
    init {
        Coroutines.io().launch {
            watcherChannel.consumeEach {
                when (it.kind) {
                    KWatchEvent.Kind.Deleted -> {
                        queue.removeFromQueue(it.file, this@FileWatcher::onFileRemoved)
                    }

                    KWatchEvent.Kind.Created, KWatchEvent.Kind.Initialized -> {
                        if (validVideoFiles().contains(it.file.extension)) {
                            queue.addToQueue(it.file, this@FileWatcher::onFilePending, this@FileWatcher::onFileAvailable)
                        } else if (it.file.isFile) {
                            logger.warn { "${it.file.name} is not a valid file type" }
                        } else if (it.file.isDirectory) {
                            val valid = it.file.walkTopDown().filter { f -> f.isFile && f.extension in validVideoFiles() }
                            logger.warn { "${it.file.name} ignoring directory" }
                        }
                    }

                    else -> {
                        logger.info { "Ignoring event kind: ${it.kind.name} for file ${it.file.name}" }
                    }
                }
            }
        }

        object : SimpleMessageListener(CommonConfig.kafkaTopic, defaultConsumer, listOf(KafkaEvents.REQUEST_FILE_READ.event)) {
            override fun onMessageReceived(data: ConsumerRecord<String, Message>) {
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

    fun validVideoFiles(): List<String> = listOf(
        "mkv",
        "avi",
        "mp4",
        "wmv",
        "webm",
        "mov"
    )


    override fun onFileAvailable(file: PendingFile) {
        logger.debug { "onFileAvailable har mottatt pendingFile ${file.file.name}" }
        val naming = Naming(file.file.nameWithoutExtension)
        val message = Message(
            referenceId = file.id,
            status = Status(StatusType.SUCCESS),
            data = FileResult(file = file.file.absolutePath, title = naming.guessDesiredTitle(), sanitizedName = naming.guessDesiredFileName())
        )
        logger.debug { "Producing message: ${Gson().toJson(message)}" }
        messageProducer.sendMessage(KafkaEvents.EVENT_READER_RECEIVED_FILE.event, message)
    }

    override fun onFilePending(file: PendingFile) {
        val message = Message(
            status = Status(StatusType.PENDING),
            data = FileResult(file = file.file.absolutePath)
        )
        messageProducer.sendMessage(KafkaEvents.EVENT_READER_RECEIVED_FILE.event , message)
    }

    override fun onFileFailed(file: PendingFile) {
        val message = Message(
            status = Status(StatusType.ERROR),
            data = file.file.absolutePath
        )
        messageProducer.sendMessage(KafkaEvents.EVENT_READER_RECEIVED_FILE.event , message)
    }

    override fun onFileRemoved(file: PendingFile) {
        val message = Message(
            status = Status(StatusType.IGNORED),
            data = file.file.absolutePath
        )
        messageProducer.sendMessage(KafkaEvents.EVENT_READER_RECEIVED_FILE.event , message)
    }



}