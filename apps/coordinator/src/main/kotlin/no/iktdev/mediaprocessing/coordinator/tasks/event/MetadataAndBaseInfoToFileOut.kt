package no.iktdev.mediaprocessing.coordinator.tasks.event

import mu.KotlinLogging
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.TaskCreator
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.datasource.toEpochSeconds
import no.iktdev.mediaprocessing.shared.common.lastOrSuccessOf
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameDeterminate
import no.iktdev.mediaprocessing.shared.common.persistance.PersistentMessage
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEnv
import no.iktdev.mediaprocessing.shared.kafka.core.KafkaEvents
import no.iktdev.mediaprocessing.shared.kafka.dto.MessageDataWrapper
import no.iktdev.mediaprocessing.shared.kafka.dto.SimpleMessageData
import no.iktdev.mediaprocessing.shared.kafka.dto.Status
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.BaseInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.MetadataPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.VideoInfoPerformed
import no.iktdev.mediaprocessing.shared.kafka.dto.events_result.hasValidData
import no.iktdev.mediaprocessing.shared.kafka.dto.isSuccess
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


/**
 *
 */
@Service
@EnableScheduling
class MetadataAndBaseInfoToFileOut(@Autowired override var coordinator: Coordinator) : TaskCreator(coordinator) {
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
        log.info { "${event.referenceId} triggered by ${event.event}" }

        val baseInfo = events.lastOrSuccessOf(KafkaEvents.EventMediaReadBaseInfoPerformed) { it.data is BaseInfoPerformed }?.data as BaseInfoPerformed?
        val meta = events.lastOrSuccessOf(KafkaEvents.EventMediaMetadataSearchPerformed) { it.data is MetadataPerformed }?.data as MetadataPerformed?

        // Only Return here as both baseInfo events are required to continue
        if (!baseInfo.isSuccess() || !baseInfo.hasValidData() || events.any { it.event == KafkaEvents.EventMediaReadOutNameAndType }) {
            return null
        }
        if (baseInfo.isSuccess() && meta == null) {
            val estimatedTimeout = LocalDateTime.now().toEpochSeconds() + metadataTimeout
            val dateTime = LocalDateTime.ofEpochSecond(estimatedTimeout, 0, ZoneOffset.UTC)

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH)
            log.info { "Sending ${baseInfo?.title} to waiting queue. Expiry ${dateTime.format(formatter)}" }
            if (!waitingProcessesForMeta.containsKey(event.referenceId)) {
                waitingProcessesForMeta[event.referenceId] = MetadataTriggerData(event.eventId, LocalDateTime.now())
            }
            return null
        }

        if (!isPrerequisiteDataPresent(events)) {
            return null
        }

        baseInfo ?: return null // Return if baseInfo is null

        val metaContentType: String? = if (meta.isSuccess()) meta?.data?.type else null
        val contentType = when (metaContentType) {
            "serie", "tv" -> FileNameDeterminate.ContentType.SERIE
            "movie" -> FileNameDeterminate.ContentType.MOVIE
            else -> FileNameDeterminate.ContentType.UNDEFINED
        }

        val fileDeterminate = if (contentType != FileNameDeterminate.ContentType.UNDEFINED) {

            val usableTitles: MutableList<String> = mutableListOf()
            meta?.data.let { mdt ->
                mdt?.title?.let { title -> usableTitles.add(title) }
                mdt?.altTitle?.let { alts -> usableTitles.addAll(alts) }
            }

            val collections = SharedConfig.outgoingContent.listFiles { it -> it.isDirectory }?.map { it.name } ?: emptyList()
            val usableCollectionAsTitle = usableTitles.mapNotNull { findNearestValue(collections, it) }

            val title = if ( usableCollectionAsTitle.isNotEmpty()) {
                val using = usableCollectionAsTitle.first()
                log.info { "Found matches in collection using: ${usableTitles.joinToString("\n")}" }
                log.info { "Using $using out of these: ${usableCollectionAsTitle.joinToString("\n")}" }
                using
            } else meta?.data?.title

            FileNameDeterminate(title ?: baseInfo.title, baseInfo.sanitizedName, contentType)
        } else FileNameDeterminate(baseInfo.title, baseInfo.sanitizedName, contentType)
        if (waitingProcessesForMeta.containsKey(event.referenceId)) {
            waitingProcessesForMeta.remove(event.referenceId)
        }

        val outputDirectory = SharedConfig.outgoingContent.using(baseInfo.title)

        val vi = fileDeterminate.getDeterminedVideoInfo()?.toJsonObject()
        return if (vi != null) {
            VideoInfoPerformed(Status.COMPLETED, vi, outDirectory = outputDirectory.absolutePath, event.eventId)
        } else {
            SimpleMessageData(Status.ERROR, "No VideoInfo found...", event.eventId)
        }
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