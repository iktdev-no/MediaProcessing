package no.iktdev.streamit.content.reader.fileWatcher

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
import no.iktdev.streamit.library.kafka.Message
import no.iktdev.streamit.library.kafka.Status
import no.iktdev.streamit.library.kafka.StatusType
import no.iktdev.streamit.library.kafka.producer.DefaultProducer
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}
@Service
class FileWatcher: FileWatcherEvents {
    val messageProducer = DefaultProducer(CommonConfig.kafkaConsumerId)

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
    }


    override fun onFileAvailable(file: PendingFile) {
        val naming = Naming(file.file.nameWithoutExtension)
        val message = Message(
            referenceId = file.id,
            status = Status(StatusType.SUCCESS),
            data = FileResult(file = file.file.absolutePath, title = naming.guessDesiredTitle(), desiredNewName = naming.guessDesiredFileName())
        )
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