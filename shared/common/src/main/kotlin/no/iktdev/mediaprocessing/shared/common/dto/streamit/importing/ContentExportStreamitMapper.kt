package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing

import no.iktdev.mediaprocessing.shared.common.model.ContentExport
import no.iktdev.mediaprocessing.shared.common.model.MediaType
import no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models.*

fun ContentExport.toMediaImportV2(): MediaImportV2 = MediaImportV2(
    reference = ImportReference(store = collection),
    metadata = metadata
        ?.takeIf { it.mediaType != MediaType.Subtitle }
        ?.let { source ->
            CatalogMetadata(
                type = when (source.mediaType) {
                    MediaType.Movie -> CatalogMetadataType.MOVIE
                    MediaType.Serie -> CatalogMetadataType.SERIE
                    MediaType.Subtitle -> error("Subtitle metadata is not catalog metadata")
                },
                title = source.title,
                alternativeTitles = source.alternativeTitles,
                genres = source.genres,
                cover = source.cover,
                summaries = source.summary.map { Summary(it.language, it.description) },
            )
        },
    media = media?.let { source ->
        Media(
            content = source.videoFile?.let { videoFile ->
                MediaContent(
                    mediaType = if (episodeInfo != null || metadata?.mediaType == MediaType.Serie) {
                        MediaContentMediaType.EPISODE
                    } else {
                        MediaContentMediaType.MOVIE
                    },
                    videoFile = videoFile,
                    episodeInfo = episodeInfo?.let {
                        EpisodeInfo(
                            season = it.seasonNumber,
                            episode = it.episodeNumber,
                            title = it.episodeTitle,
                        )
                    },
                )
            },
            subtitles = source.subtitles.map { subtitle ->
                val format = subtitle.subtitleFile.substringAfterLast('.', "").lowercase()
                require(format.isNotBlank()) {
                    "Subtitle file '${subtitle.subtitleFile}' has no file extension for Streamit import format"
                }
                SubtitleImport(
                    subtitleFile = subtitle.subtitleFile,
                    language = subtitle.language,
                    format = format,
                )
            },
        )
    },
)
