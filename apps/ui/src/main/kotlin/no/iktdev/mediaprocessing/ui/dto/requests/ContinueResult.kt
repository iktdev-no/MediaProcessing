package no.iktdev.mediaprocessing.ui.dto.requests

sealed interface ContinueResult {
    data object ContinueSuccess : ContinueResult
    data class ContinueFailure(val message: String) : ContinueResult
}
