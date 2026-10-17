package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models

import kotlin.String

data class SubtitleImport(
  val subtitleFile: String,
  val language: String,
  val format: String,
)
