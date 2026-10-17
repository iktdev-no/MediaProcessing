package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models

import kotlin.String
import kotlin.collections.List

data class CatalogMetadata(
  val title: String? = null,
  val alternativeTitles: List<String>,
  val genres: List<String>,
  val cover: String? = null,
  val type: CatalogMetadataType,
  val summaries: List<Summary>,
)
