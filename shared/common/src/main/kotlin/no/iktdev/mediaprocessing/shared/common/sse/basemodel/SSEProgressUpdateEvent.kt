package no.iktdev.mediaprocessing.shared.common.sse.basemodel

import no.iktdev.mediaprocessing.shared.common.model.ProgressUpdate
import no.iktdev.mediaprocessing.shared.common.sse.SSEEvent
import no.iktdev.mediaprocessing.shared.common.sse.SSEKeys

data class SSEProgressUpdateEvent(val progress: ProgressUpdate): SSEEvent {
    override val type: String = SSEKeys.Progress.key
}