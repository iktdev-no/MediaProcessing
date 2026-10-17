package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models

import kotlin.Int
import kotlin.String

data class EpisodeInfo(
  val season: Int,
  val episode: Int,
  val title: String? = null,
)
