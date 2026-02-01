import type { EventQuery } from "../../types/backendTypes"
import { eventFilterSchema } from "./eventFilterSchema"

export function parseEventFilters(filters: string[], base: EventQuery): EventQuery {
    const next: EventQuery = { ...base }

    // Multi-key
    const keyChips = filters.filter(f => f.startsWith("key:"))
    next.key = keyChips.length > 0
        ? keyChips.map(k => k.substring(4))
        : undefined

    // eventId
    next.eventId = filters.find(f =>
        eventFilterSchema.eventIds.includes(f)
    )

    // referenceId (UUID)
    next.referenceId = filters.find(f =>
        /^[0-9a-fA-F-]{36}$/.test(f)
    )

    // from:2024-01-01
    const fromChip = filters.find(f => f.startsWith("from:"))
    if (fromChip) next.from = fromChip.substring(5)

    // to:2024-01-31
    const toChip = filters.find(f => f.startsWith("to:"))
    if (toChip) next.to = toChip.substring(3)

    return next
}
