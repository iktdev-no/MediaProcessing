package no.iktdev.mediaprocessing.shared.common.sse.basemodel

import no.iktdev.mediaprocessing.shared.common.sse.SSEEvent
import no.iktdev.mediaprocessing.shared.common.sse.SSEKeys

data class SSEPingEvent(
    val timestamp: Long = System.currentTimeMillis()
) : SSEEvent {
    override val type: String = SSEKeys.Ping.key
}