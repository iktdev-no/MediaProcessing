package no.iktdev.mediaprocessing.transferModel.coordinatorUi.preference.coordinator

data class CoordinatorPreference(
    val media: MediaPreference,
    val language: LanguagePreference,
    val cleanup: CleanupPreference = CleanupPreference.default(),
)