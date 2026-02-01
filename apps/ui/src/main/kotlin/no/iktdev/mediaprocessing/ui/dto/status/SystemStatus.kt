package no.iktdev.mediaprocessing.ui.dto.status

data class SystemStatus(
    var coordinatorRest: Boolean = false,
    var coordinatorSse: Boolean = false,
    var processer: Boolean = false,
    var converter: Boolean = false,
    var pyMetadata: Boolean = false,
    var pyWatcher: Boolean = false,
    var interval: Long = 0L,
    var timestamp: Long = 0L
)
