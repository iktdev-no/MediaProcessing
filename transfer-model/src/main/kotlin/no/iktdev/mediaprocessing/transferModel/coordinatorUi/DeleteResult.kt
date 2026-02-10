package no.iktdev.mediaprocessing.transferModel.coordinatorUi

sealed interface DeleteResult {
    data object Success : DeleteResult
    data class Failure(val message: String) : DeleteResult
}