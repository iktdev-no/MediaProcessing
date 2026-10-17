package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models


data class MediaImportV2(
  val reference: ImportReference? = null,
  val metadata: CatalogMetadata? = null,
  val media: Media? = null,
)
