package no.iktdev.mediaprocessing.shared.common.dto.streamit.importing.models

import com.fasterxml.jackson.`annotation`.JsonValue
import kotlin.String
import kotlin.collections.Map

enum class MediaContentMediaType(
  @JsonValue
  val `value`: String,
) {
  MOVIE("Movie"),
  EPISODE("Episode"),
  ;

  companion object {
    private val mapping: Map<String, MediaContentMediaType> =
        entries.associateBy(MediaContentMediaType::value)

    fun fromValue(`value`: String): MediaContentMediaType? = mapping[value]
  }
}
