package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models

import com.fasterxml.jackson.`annotation`.JsonValue
import kotlin.String
import kotlin.collections.Map

enum class CatalogMetadataType(
  @JsonValue
  val `value`: String,
) {
  MOVIE("Movie"),
  SERIE("Serie"),
  ;

  companion object {
    private val mapping: Map<String, CatalogMetadataType> =
        entries.associateBy(CatalogMetadataType::value)

    fun fromValue(`value`: String): CatalogMetadataType? = mapping[value]
  }
}
