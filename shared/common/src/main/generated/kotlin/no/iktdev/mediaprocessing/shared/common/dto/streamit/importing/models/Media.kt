package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models

import kotlin.collections.List

data class Media(
  val content: MediaContent? = null,
  val subtitles: List<SubtitleImport>,
)
