package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models

import kotlin.Long
import kotlin.String

data class ImportReference(
  val catalogId: Long? = null,
  val store: String,
)
