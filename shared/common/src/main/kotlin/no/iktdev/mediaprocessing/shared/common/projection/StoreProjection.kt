package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MigrateContentToStoreTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.MigrateStatus
import java.io.File

class StoreProjection(val events: List<Event>) {

    fun projectMetadata(): ContentExport.MetadataExport? {
        val metadata = CollectProjection(events).metadata
        if (metadata != null) {
            val useCover = if (metadata.cover != null) {
                val migrated = events.filterIsInstance<MigrateContentToStoreTaskResultEvent>().lastOrNull { it.status == TaskStatus.Completed }?.coverMigrate ?: emptyList()
                migrated.filter { it.status == MigrateStatus.Completed && it.storedUri != null }
                    .map { File(it.storedUri!!).name }
                    .find { it == metadata.cover.name }
            } else null

            return ContentExport.MetadataExport(
                title = metadata.title,
                genres = metadata.genres,
                cover = useCover,
                summary = metadata.summary,
                mediaType = metadata.mediaType,
                source = metadata.source
            )
        } else {
            val parsedInfo = events.filterIsInstance<MediaParsedInfoEvent>().lastOrNull() ?: return null
            return ContentExport.MetadataExport(
                title = parsedInfo.data.parsedCollection,
                mediaType = parsedInfo.data.mediaType,
            )
        }
    }

    fun projectEpisodeInfo(): ContentExport.EpisodeInfo? {
        val episodeInfo = events.filterIsInstance<MediaParsedInfoEvent>().lastOrNull()?.data?.episodeInfo ?: return null
        return ContentExport.EpisodeInfo(
            episodeNumber = episodeInfo.episodeNumber,
            seasonNumber = episodeInfo.seasonNumber,
            episodeTitle = episodeInfo.episodeTitle,
        )
    }

    fun projectMediaFiles(): ContentExport.MediaExport? {
        val migrated = events.filterIsInstance<MigrateContentToStoreTaskResultEvent>().lastOrNull { it.status == TaskStatus.Completed }
        return ContentExport.MediaExport(
            videoFile = migrated?.videoMigrate?.let { video ->
                if (video.status == MigrateStatus.Completed) File(video.storedUri!!).name else null
            },
            subtitles = migrated?.subtitleMigrate?.filter { it.status == MigrateStatus.Completed }
                ?.map { ContentExport.MediaExport.Subtitle(subtitleFile = File(it.storedUri!!).name, language = it.language!!) } ?: emptyList()
        )
    }

    fun getCollection(): String? {
        val migrated = events.filterIsInstance<MigrateContentToStoreTaskResultEvent>().lastOrNull { it.status == TaskStatus.Completed } ?: return null
        return if (migrated.status == TaskStatus.Completed) migrated.collection else null
    }

}