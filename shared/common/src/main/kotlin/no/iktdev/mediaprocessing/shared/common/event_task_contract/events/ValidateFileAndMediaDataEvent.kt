package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event


data class ValidateFileAndMediaDataEvent(
    val validationStatus: ValidationStatus,
    val rejectionReason: String? = null,
    val warnings: List<String> = emptyList(),
    val severity: ValidationSeverity = ValidationSeverity.None
) : Event() {

    enum class ValidationStatus {
        Ok,
        Rejected
    }

    enum class ValidationSeverity {
        None,
        Warning,
        Critical
    }
}
