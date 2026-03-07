package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.TaskStatus
import no.iktdev.exfl.using
import no.iktdev.mediaprocessing.shared.common.cleanForFileSystemUse
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ConvertTaskResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.CoverDownloadResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MediaParsedInfoEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.MetadataSearchResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserEncodeResultEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ProcesserExtractResultEvent
import no.iktdev.mediaprocessing.shared.common.getInstanceOf
import no.iktdev.mediaprocessing.shared.common.getInstancesOf
import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.ContentMigrationPlan
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import java.io.File

class SummaryProjection(
    val collection: String,
    val events: List<Event>,
    val outbox: File
) {

    val useStore: File = outbox.using(collection)

    fun projectMetadata(plan: ContentMigrationPlan): ContentExport.MetadataExport? {
        val metadata = CollectProjection(events).metadata
        if (metadata != null) {
            return ContentExport.MetadataExport(
                title = metadata.title,
                alternativeTitles = metadata.alternativeTitles,
                genres = metadata.genres,
                cover = plan.coverContent?.storeUri?.let { File(it).name },
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


    fun createMigrationPlan(): ContentMigrationPlan {
        return ContentMigrationPlan(
            collection = collection,
            coverContent = getCoverMigration(),
            videoContent = getVideoMigration(),
            subtitleContent = getSubtitleMigration()
        )
    }


    /**
     * Returns the basename for either a cover or movie (video) file
     */
    fun getFileName(): String {
        val parsed = events.getInstanceOf<MediaParsedInfoEvent>() ?: throw IllegalStateException("No media event configured for migration plan found")
        return parsed.data.parsedFileName.cleanForFileSystemUse()
    }

    fun getCoverMigration(): ContentMigrationPlan.SingleContent? {
        val useCover = events.getInstancesOf<CoverDownloadResultEvent>()
            .filter { it -> it.status == TaskStatus.Completed }
            .lastOrNull { it.data != null } ?: return null

        val cachedFile = useCover.data!!.outputFile.let(::File)

        val useName = (if (isMovie()) getFileName() else {
            collection.cleanForFileSystemUse()
        }).let { name -> "$name.${cachedFile.extension}" }

        val storeFile = useStore.using(useName)
        return ContentMigrationPlan.SingleContent(
            cachedFile.absolutePath,
            storeFile.absolutePath
        )
    }

    fun getVideoMigration(): ContentMigrationPlan.SingleContent? {
        val cachedFile = events.getInstancesOf<ProcesserEncodeResultEvent>()
            .lastOrNull()?.data?.cachedOutputFile?.let(::File) ?: return null

        val filename = getFileName().let { "$it.${cachedFile.extension}" }
        val storeFile = useStore.using(filename)

        return ContentMigrationPlan.SingleContent(
            cachedFile.absolutePath,
            storeFile.absolutePath
        )
    }

    fun getSubtitleMigration(): List<ContentMigrationPlan.SingleSubtitle>? {
        val baseName = getFileName()
        val store = useStore

        // Samle alle (language, file) par i én passering
        val allSubtitleFiles = events.flatMap { event ->
            when (event) {
                is ProcesserExtractResultEvent ->
                    event.data?.let { listOf(it.language to File(it.cachedOutputFile)) } ?: emptyList()

                is ConvertTaskResultEvent ->
                    event.data?.outputFiles
                        ?.map { event.data.language to File(it) }
                        ?: emptyList()

                else -> emptyList()
            }
        }

        if (allSubtitleFiles.isEmpty()) return null

        // Gruppér etter språk
        val grouped = allSubtitleFiles.groupBy({ it.first }, { it.second })

        // Bygg resultat
        return grouped.flatMap { (language, files) ->
            files.map { cached ->
                val filename = "$baseName.${cached.extension}"
                val storeFile = store.using("sub", language, filename)

                ContentMigrationPlan.SingleSubtitle(
                    language = language,
                    cachedUri = cached.absolutePath,
                    storeUri = storeFile.absolutePath,
                )
            }
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

    fun projectMediaFiles(plan: ContentMigrationPlan): ContentExport.MediaExport? {
        return ContentExport.MediaExport(
            videoFile = getVideoFile(plan),
            subtitles = getSubtitleFile(plan)
        )
    }

    fun getCover(plan: ContentMigrationPlan): String? {
        return plan.coverContent?.storeUri?.let { x -> File(x).name }
    }

    fun getVideoFile(plan: ContentMigrationPlan): String? {
        return plan.videoContent?.storeUri?.let { x -> File(x).name }
    }

    fun getSubtitleFile(plan: ContentMigrationPlan): List<ContentExport.MediaExport.Subtitle> {
        return plan.subtitleContent?.map { it ->
            ContentExport.MediaExport.Subtitle(subtitleFile = it.storeUri.let { File(it).name }, language = it.language)
        } ?: emptyList()
    }


    fun isMovie(): Boolean {
        val parsedType = events
            .getInstanceOf<MediaParsedInfoEvent>()
            ?.data
            ?.mediaType

        val metadataType = events
            .filterIsInstance<MetadataSearchResultEvent>()
            .lastOrNull()
            ?.recommended
            ?.metadata
            ?.type

        // Velg første ikke-null, og sjekk om den er Movie
        return (parsedType ?: metadataType) == MediaType.Movie
    }

    data class CachedToStore(val cachedFile: File, val storeFile: File)
    data class CachedToStoreLanguage(val cts: CachedToStore, val language: String)
}