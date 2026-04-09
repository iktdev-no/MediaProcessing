package no.iktdev.mediaprocessing.shared.common.event_task_contract.events

import no.iktdev.eventi.models.Event
import java.util.UUID

data class AlterOverrideEvent(
    val targetEventId: UUID,
    val overrides: List<String> // Enum value, should be serializable
): Event() {}