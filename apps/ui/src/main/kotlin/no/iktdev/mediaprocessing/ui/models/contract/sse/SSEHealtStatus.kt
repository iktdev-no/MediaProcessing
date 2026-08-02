package no.iktdev.mediaprocessing.ui.models.contract.sse

import no.iktdev.mediaprocessing.shared.common.sse.SSEEvent
import no.iktdev.mediaprocessing.shared.common.sse.SSEKeys
import no.iktdev.mediaprocessing.ui.models.contract.SystemStatus

data class SSEHealthStatus(val systemHealth: SystemStatus): SSEEvent {
    override val type = SSEKeys.HealthStatus.key
}