package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.SignalEvent

data class ReleaseHoldSignalEvent(
    val reason: String? = null,
): SignalEvent() {}