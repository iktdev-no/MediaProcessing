package no.iktdev.mediaprocessing.ui.dto.requests

sealed interface ContinueResult {
    data object Success : ContinueResult
    data class Failure(val message: String) : ContinueResult
}
