package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models

import kotlin.String

data class MediaContent(
  val mediaType: MediaContentMediaType,
  val videoFile: String,
  val episodeInfo: EpisodeInfo? = null,
)
