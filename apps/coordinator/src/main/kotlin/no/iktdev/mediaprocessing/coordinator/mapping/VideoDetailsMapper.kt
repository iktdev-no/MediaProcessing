package no.iktdev.mediaprocessing.coordinator.mapping

import no.iktdev.mediaprocessing.shared.contract.data.Event
import no.iktdev.mediaprocessing.shared.contract.reader.SerieInfo
import no.iktdev.mediaprocessing.shared.contract.reader.VideoDetails

class VideoDetailsMapper(val events: List<Event>) {

   /* fun mapTo(): VideoDetails? {
        val mediaReadOut = events.lastOrNull { it.data is VideoInfoPerformed }?.data as VideoInfoPerformed?
        val proper = mediaReadOut?.toValueObject() ?: return null

        val details = VideoDetails(
            type = proper.type,
            fullName = proper.fullName,
            serieInfo = if (proper !is EpisodeInfo) null else SerieInfo(
                episodeTitle = proper.episodeTitle,
                episodeNumber = proper.episode,
                seasonNumber = proper.season,
                title = proper.title
            )
        )
        return details
    }*/
}