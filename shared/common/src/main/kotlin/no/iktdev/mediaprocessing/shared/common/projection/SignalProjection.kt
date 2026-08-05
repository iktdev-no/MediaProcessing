package no.iktdev.mediaprocessing.shared.common.projection

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.SignalEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.OnHoldSignalEvent
import no.iktdev.mediaprocessing.shared.common.event_task_contract.events.ReleaseHoldSignalEvent
import kotlin.reflect.KClass

class SignalProjection(
    private val events: List<Event>
) {

    private val signals = events.filterIsInstance<SignalEvent>()

    val lastSignal: SignalEvent? =
        signals.maxByOrNull { it.metadata.created }

    fun hasRelevantSignal(vararg signalClasses: KClass<out SignalEvent>): Boolean {
        return signals.any { signal -> signal::class in signalClasses }
    }

    val isOnHold: Boolean =
        lastSignal is OnHoldSignalEvent

    val isReleased: Boolean =
        lastSignal is ReleaseHoldSignalEvent

    val holdReason: String? =
        (lastSignal as? OnHoldSignalEvent)?.reason

    val releaseReason: String? =
        (lastSignal as? ReleaseHoldSignalEvent)?.reason
}
