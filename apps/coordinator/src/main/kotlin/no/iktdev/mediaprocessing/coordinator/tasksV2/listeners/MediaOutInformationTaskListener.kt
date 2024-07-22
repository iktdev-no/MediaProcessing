package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.JsonObject
import no.iktdev.eventi.core.ConsumableEvent
import no.iktdev.eventi.core.WGson
import no.iktdev.eventi.data.EventStatus
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.utils.log
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameDeterminate
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.mediaprocessing.shared.common.parsing.Regexes
import no.iktdev.mediaprocessing.shared.common.parsing.isCharOnlyUpperCase
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.data.*
import no.iktdev.mediaprocessing.shared.contract.data.EpisodeInfo
import no.iktdev.mediaprocessing.shared.contract.data.MovieInfo
import no.iktdev.mediaprocessing.shared.contract.data.pyMetadata
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.FileFilter
import javax.naming.Name


@Service
class MediaOutInformationTaskListener: CoordinatorEventListener() {

    override fun getProducerName(): String {
        return this::class.java.simpleName
    }

    @Autowired
    override var coordinator: Coordinator? = null

    override val produceEvent: Events = Events.EventMediaReadOutNameAndType
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaMetadataSearchPerformed
    )

    override fun shouldIHandleFailedEvents(incomingEvent: Event): Boolean {
        return (incomingEvent.eventType in listensForEvents)
    }

    override fun onEventsReceived(incomingEvent: ConsumableEvent<Event>, events: List<Event>) {
        val event = incomingEvent.consume()
        if (event == null) {
            log.error { "Event is null and should not be available! ${WGson.gson.toJson(incomingEvent.metadata())}" }
            return
        }

        val metadataResult = event.az<MediaMetadataReceivedEvent>()
        val mediaBaseInfo = events.findLast { it.eventType == Events.EventMediaReadBaseInfoPerformed }?.az<BaseInfoEvent>()?.data
        if (mediaBaseInfo == null) {
            log.error { "Required event ${Events.EventMediaReadBaseInfoPerformed} is not present" }
            coordinator?.produceNewEvent(
                MediaOutInformationConstructedEvent(
                    metadata = event.makeDerivedEventInfo(EventStatus.Failed, getProducerName())
                )
            )
            return
        }
        val pm = ProcessMediaInfoAndMetadata(mediaBaseInfo, metadataResult?.data)
        val vi = pm.getVideoPayload()

        val result = if (vi != null) {
            MediaInfoReceived(
                outDirectory = pm.getOutputDirectory().absolutePath,
                info = vi
            ).let { MediaOutInformationConstructedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Success, getProducerName()),
                data = it
            ) }
        } else {
            MediaOutInformationConstructedEvent(
                metadata = event.makeDerivedEventInfo(EventStatus.Failed, getProducerName())
            )
        }
        onProduceEvent(result)
    }

    class ProcessMediaInfoAndMetadata(val baseInfo: BaseInfo, val metadata: pyMetadata? = null) {
        var metadataDeterminedContentType: FileNameDeterminate.ContentType = metadata?.type?.let { contentType ->
            when (contentType) {
                "serie", "tv" -> FileNameDeterminate.ContentType.SERIE
                "movie" -> FileNameDeterminate.ContentType.MOVIE
                else -> FileNameDeterminate.ContentType.UNDEFINED
            }
        } ?: FileNameDeterminate.ContentType.UNDEFINED

        fun getTitlesFromMetadata(): List<String> {
            val titles: MutableList<String> = mutableListOf()
            metadata?.title?.let { titles.add(it) }
            metadata?.altTitle?.let { titles.addAll(it) }
            return titles
        }
        fun getExistingCollections() = SharedConfig.outgoingContent.listFiles(FileFilter { it.isDirectory })?.map { it.name } ?: emptyList()

        fun getUsedCollectionForTitleOrNull(): String? {
            val exisiting = getExistingCollections()
            if (exisiting.isEmpty()) {
                return null
            }
            // Contains will cause mayhem!, ex: Collection with a single letter will make it messy
            val existingMatch = exisiting.find { it.lowercase() == baseInfo.title.lowercase() }
            return existingMatch
        }

        fun getCollection(): String {
            val usedCollection = getUsedCollectionForTitleOrNull()
            if (usedCollection != null) {
                return usedCollection
            }

            val metaTitles = getTitlesFromMetadata()
            val existingCollection = getExistingCollections()

            // Contains will cause mayhem!, ex: Collection with a single letter will make it messy
            val ecList = existingCollection.filter { ec -> metaTitles.any { it.lowercase() == ec.lowercase() } }
            if (ecList.isNotEmpty()) {
                return ecList.first()
            }


            return NameHelper.cleanup(baseInfo.title)
        }

        fun getTitle(): String {
            val metaTitles = getTitlesFromMetadata()
            val collection = getCollection()

            val filteredMetaTitles = metaTitles.filter { it.lowercase().contains(baseInfo.title.lowercase()) || NameHelper.normalize(it).lowercase().contains(baseInfo.title.lowercase()) }

            //val viableFileTitles = filteredMetaTitles.filter { !it.isCharOnlyUpperCase() }

            return if (collection == baseInfo.title) {
                collection
            } else {
                NameHelper.cleanup (filteredMetaTitles.firstOrNull() ?: baseInfo.title)
            }
        }

        fun getVideoPayload(): JsonObject? {
            val defaultFnd = FileNameDeterminate(getTitle(), baseInfo.sanitizedName, FileNameDeterminate.ContentType.UNDEFINED)

            val determinedContentType = defaultFnd.getDeterminedVideoInfo()
                .let {
                    when (it) {
                        is EpisodeInfo -> FileNameDeterminate.ContentType.SERIE
                        is MovieInfo -> FileNameDeterminate.ContentType.MOVIE
                        else -> FileNameDeterminate.ContentType.UNDEFINED
                    }
                }
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
}