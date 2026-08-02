package no.iktdev.mediaprocessing.ui

import no.iktdev.eventi.models.Event
import no.iktdev.eventi.models.store.PersistedEvent
import no.iktdev.eventi.models.store.PersistedTask
import no.iktdev.eventi.serialization.WGson
import no.iktdev.eventi.serialization.ZDS.toEvent
import no.iktdev.mediaprocessing.shared.common.rules.TaskLifecycleRules
import no.iktdev.mediaprocessing.ui.models.contract.UiEvent
import no.iktdev.mediaprocessing.ui.models.contract.UiTask


fun List<PersistedEvent>.toEvents(): List<Event> {
    return this.mapNotNull { it.toEvent() }
}

fun List<PersistedEvent>.toUiEvents(): List<UiEvent> {
    return this.toEvents().map { it.toUIEvent() }
}

fun Event.toUIEvent(): UiEvent {
    return UiEvent(
        referenceId = this.referenceId,
        eventId = this.eventId,
        event = this::class.simpleName ?: run {
            throw IllegalStateException("Missing class name for event: $this")
        },
        data = WGson.gson.toJson(this),
        persistedAt = this.metadata.created,
        derivedOf = this.metadata.derivedFromId
    )
}

fun PersistedTask.toUITask(logs: List<String> = emptyList()): UiTask {
    val overrides = this.getOverrides()
    return UiTask(
        taskId = this.taskId,
        referenceId = this.referenceId,
        status = this.status.name,
        task = this.task,
        data = this.data,
        claimed = this.claimed,
        claimedBy = this.claimedBy,
        consumed = this.consumed,
        lastCheckIn = this.lastCheckIn,
        persistedAt = this.persistedAt,
        abandoned = TaskLifecycleRules.isAbandoned(consumed, persistedAt, lastCheckIn),
        logFiles = logs,
        availableOverrides = overrides?.available ?: emptyList(),
        activeOverrides = overrides?.active ?: emptyList(),
    )
}