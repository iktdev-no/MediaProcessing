package no.iktdev.mediaprocessing.ui.models.contract.preferences

import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.CleanupPreference
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.LanguagePreference
import no.iktdev.mediaprocessing.ui.models.contract.preferences.coordinator.MediaPreference
import no.iktdev.mediaprocessing.ui.models.contract.preferences.processer.CPULimit


data class CoordinatorPreference(
    val media: MediaPreference,
    val language: LanguagePreference,
    val cleanup: CleanupPreference,
)

data class ProcessorPreference(
    val cpuLimit: CPULimit
)