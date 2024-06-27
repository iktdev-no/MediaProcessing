package no.iktdev.mediaprocessing.coordinator.tasks.event

import com.google.gson.JsonObject
import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.EventCoordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.datasource.toEpochSeconds
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameDeterminate
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.mediaprocessing.shared.common.parsing.Regexes
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.*
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.FileFilter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


/**
 *
 */
@Service
@EnableScheduling
class MetadataAndBaseInfoToFileOut(@Autowired override var coordinator: EventCoordinator) : TaskCreator(coordinator) {
    val log = KotlinLogging.logger {}
    val metadataTimeout = KafkaEnv.metadataTimeoutMinutes * 60

    override val producesEvent: KafkaEvents
        get() = KafkaEvents.EventMediaReadOutNameAndType

    val waitingProcessesForMeta: MutableMap<String, MetadataTriggerData> = mutableMapOf()

    override val listensForEvents: List<KafkaEvents> = listOf(
        KafkaEvents.EventMediaReadBaseInfoPerformed,
        KafkaEvents.EventMediaMetadataSearchPerformed
    )

    override fun onProcessEvents(event: PersistentMessage, events: List<PersistentMessage>): MessageDataWrapper? {
        super.onProcessEventsAccepted(event, events)

        log.info { "${event.referenceId} triggered by ${event.event}" }

        val baseInfo = events.lastOrSuccessOf(KafkaEvents.EventMediaReadBaseInfoPerformed) { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed? ?: return null
        val meta = events.lastOrSuccessOf(KafkaEvents.EventMediaMetadataSearchPerformed) { it.data is MetadataPerformed }?.data as MetadataPerformed?

        // Only Return here as both baseInfo events are required to continue
        if (!baseInfo.isSuccess() || !baseInfo.hasValidData() || events.any { it.event == KafkaEvents.EventMediaReadOutNameAndType }) {
            return null
        }
        if (baseInfo.isSuccess() && meta == null) {
            val estimatedTimeout = LocalDateTime.now().toEpochSeconds() + metadataTimeout
            val dateTime = LocalDateTime.ofEpochSecond(estimatedTimeout, 0, ZoneOffset.UTC)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)
            log.info { "Sending ${baseInfo.title} to waiting queue. Expiry ${dateTime.format(formatter)}" }
            if (!waitingProcessesForMeta.containsKey(event.referenceId)) {
                waitingProcessesForMeta[event.referenceId] = MetadataTriggerData(event.eventId, LocalDateTime.now())
            }
            return null
        }

        if (!isPrerequisiteDataPresent(events)) {
            return null
        }

        if (waitingProcessesForMeta.containsKey(event.referenceId)) {
            waitingProcessesForMeta.remove(event.referenceId)
        }

        val pm = ProcessMediaInfoAndMetadata(baseInfo, meta)


        val vi = pm.getVideoPayload()
        return if (vi != null) {
            VideoInfoPerformed(Status.COMPLETED, vi, outDirectory = pm.getOutputDirectory().absolutePath, event.eventId)
        } else {
            SimpleMessageData(Status.ERROR, "No VideoInfo found...", event.eventId)
        }
    }


    class ProcessMediaInfoAndMetadata(val baseInfo: BaseInfoPerformed, val metadata: MetadataPerformed? = null) {
        var metadataDeterminedContentType: FileNameDeterminate.ContentType = metadata?.data?.type?.let { contentType ->
            when (contentType) {
                "serie", "tv" -> FileNameDeterminate.ContentType.SERIE
                "movie" -> FileNameDeterminate.ContentType.MOVIE
                else -> FileNameDeterminate.ContentType.UNDEFINED
            }
        } ?: FileNameDeterminate.ContentType.UNDEFINED

        fun getTitlesFromMetadata(): List<String> {
            val titles: MutableList<String> = mutableListOf()
            metadata?.data?.title?.let { titles.add(it) }
            metadata?.data?.altTitle?.let { titles.addAll(it) }
            return titles
        }
        fun getExistingCollections() =
            SharedConfig.outgoingContent.listFiles(FileFilter { it.isDirectory })?.map { it.name } ?: emptyList()

        fun getAlreadyUsedForCollectionOrTitle(): String {
            val exisiting = getExistingCollections()
            val existingMatch = exisiting.find { it.contains(baseInfo.title) }
            if (existingMatch != null) {
                return existingMatch
            }
            val metaTitles = getTitlesFromMetadata()
            return metaTitles.firstOrNull { it.contains(baseInfo.title) }
                ?: (getTitlesFromMetadata().firstOrNull { it in exisiting } ?: getTitlesFromMetadata().firstOrNull()
                ?: baseInfo.title)
        }

        fun getCollection(): String {
            val title = getAlreadyUsedForCollectionOrTitle()?: metadata?.data?.title ?: baseInfo.title
            var cleaned = Regexes.illegalCharacters.replace(title, " - ")
            cleaned = Regexes.trimWhiteSpaces.replace(cleaned, " ")
            return cleaned
        }

        fun getTitle(): String {
            val metaTitles = getTitlesFromMetadata()
            val metaTitle = metaTitles.filter { it.contains(baseInfo.title) || NameHelper.normalize(it).contains(baseInfo.title) }
            val title = metaTitle.firstOrNull() ?: metaTitles.firstOrNull() ?: baseInfo.title
            var cleaned = Regexes.illegalCharacters.replace(title, " - ")
            cleaned = Regexes.trimWhiteSpaces.replace(cleaned, " ")
            return cleaned
        }

        fun getVideoPayload(): JsonObject? {
            val defaultFnd = FileNameDeterminate(getTitle(), baseInfo.sanitizedName, FileNameDeterminate.ContentType.UNDEFINED)

            val determinedContentType = defaultFnd.getDeterminedVideoInfo().let { if (it is EpisodeInfo) FileNameDeterminate.ContentType.SERIE else if (it is MovieInfo) FileNameDeterminate.ContentType.MOVIE else FileNameDeterminate.ContentType.UNDEFINED }
            return if (determinedContentType == metadataDeterminedContentType && determinedContentType == FileNameDeterminate.ContentType.MOVIE) {
                FileNameDeterminate(getTitle(), getTitle(), FileNameDeterminate.ContentType.MOVIE).getDeterminedVideoInfo()?.toJsonObject()
            } else {
                FileNameDeterminate(getTitle(), baseInfo.sanitizedName, metadataDeterminedContentType).getDeterminedVideoInfo()?.toJsonObject()
            }
        }

        fun getOutputDirectory() = SharedConfig.outgoingContent.using(NameHelper.normalize(getCollection()))



    }


    fun findNearestValue(list: List<String>, target: String): String? {
        return list.minByOrNull { it.distanceTo(target) }
    }

    fun String.distanceTo(other: String): Int {
        val distance = Array(length + 1) { IntArray(other.length + 1) }
        for (i in 0..length) {
            distance[i][0] = i
        }
        for (j in 0..other.length) {
            distance[0][j] = j
        }
        for (i in 1..length) {
            for (j in 1..other.length) {
                distance[i][j] = minOf(
                    distance[i - 1][j] + 1,
                    distance[i][j - 1] + 1,
                    distance[i - 1][j - 1] + if (this[i - 1] == other[j - 1]) 0 else 1
                )
            }
        }
        return distance[length][other.length]
    }

    //@Scheduled(fixedDelay = (60_000))
    @Scheduled(fixedDelay = (1_000))
    fun sendErrorMessageForMetadata() {
        val expired = waitingProcessesForMeta.filter {
            LocalDateTime.now().toEpochSeconds() > (it.value.executed.toEpochSeconds() + metadataTimeout)
        }
        expired.forEach {
            log.info { "Producing timeout for ${it.key} ${LocalDateTime.now()}" }
            producer.sendMessage(it.key, KafkaEvents.EventMediaMetadataSearchPerformed, MetadataPerformed(status = Status.ERROR, "Timed Out by: ${this@MetadataAndBaseInfoToFileOut::class.simpleName}", derivedFromEventId = it.value.eventId))
            waitingProcessesForMeta.remove(it.key)
        }
    }

    data class MetadataTriggerData(val eventId: String, val executed: LocalDateTime)

}