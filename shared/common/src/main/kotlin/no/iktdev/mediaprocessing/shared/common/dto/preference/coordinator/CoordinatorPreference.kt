package no.iktdev.mediaprocessing.shared.common.dto.preference.coordinator

data class CoordinatorPreference(
    val media: MediaPreference,
    val language: LanguagePreference,
    val cleanup: CleanupPreference = CleanupPreference.default(),
)