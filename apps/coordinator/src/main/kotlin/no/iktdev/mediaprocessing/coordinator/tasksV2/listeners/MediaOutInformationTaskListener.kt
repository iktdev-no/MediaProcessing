package no.iktdev.mediaprocessing.coordinator.tasksV2.listeners

import com.google.gson.JsonObject
import no.iktdev.eventi.data.EventStatus
import no.iktdev.eventi.implementations.EventCoordinator
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.coordinator.Coordinator
import no.iktdev.mediaprocessing.coordinator.CoordinatorEventListener
import no.iktdev.mediaprocessing.coordinator.utils.log
import no.iktdev.mediaprocessing.shared.common.SharedConfig
import no.iktdev.mediaprocessing.shared.common.parsing.FileNameDeterminate
import no.iktdev.mediaprocessing.shared.common.parsing.NameHelper
import no.iktdev.mediaprocessing.shared.common.parsing.Regexes
import no.iktdev.mediaprocessing.shared.contract.Events
import no.iktdev.mediaprocessing.shared.contract.EventsListenerContract
import no.iktdev.mediaprocessing.shared.contract.EventsManagerContract
import no.iktdev.mediaprocessing.shared.contract.data.*
import no.iktdev.mediaprocessing.shared.contract.data.EpisodeInfo
import no.iktdev.mediaprocessing.shared.contract.data.MovieInfo
import no.iktdev.mediaprocessing.shared.contract.data.pyMetadata
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.FileFilter


@Service
class MediaOutInformationTaskListener: CoordinatorEventListener() {
    @Autowired
    override var coordinator: Coordinator? = null

    override val produceEvent: Events = Events.EventMediaReadOutNameAndType
    override val listensForEvents: List<Events> = listOf(
        Events.EventMediaMetadataSearchPerformed
    )

    override fun onEventsReceived(incomingEvent: Event, events: List<Event>) {
        val metadataResult = incomingEvent.az<MediaMetadataReceivedEvent>()
        val mediaBaseInfo = events.findLast { it.eventType == Events.EventMediaReadBaseInfoPerformed }?.az<BaseInfoEvent>()?.data
        if (mediaBaseInfo == null) {
            log.error { "Required event ${Events.EventMediaReadBaseInfoPerformed} is not present" }
            coordinator?.produceNewEvent(
                MediaOutInformationConstructedEvent(
                    metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed)
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
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Success),
                data = it
            ) }
        } else {
            MediaOutInformationConstructedEvent(
                metadata = incomingEvent.makeDerivedEventInfo(EventStatus.Failed)
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
            val title = getAlreadyUsedForCollectionOrTitle()?: metadata?.title ?: baseInfo.title
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
}