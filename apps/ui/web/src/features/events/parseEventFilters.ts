import type { EventQuery } from "../../types/webTypes"

export function parseEventFilters(filters: string[], base: EventQuery): EventQuery {
    const next: EventQuery = { ...base }

    //
    // Multi-key
    //
    const keyChips = filters.filter(f => f.startsWith("key:"))
    next.key = keyChips.length > 0
        ? keyChips.map(k => k.substring(4))
        : undefined

    //
    // eventId (eksisterende)
    //
    next.eventId = filters.find(f =>
        /^[A-Za-z0-9_-]+$/.test(f) &&
        f.length > 0 &&
        f.length < 64 && // litt heuristikk
        f.toLowerCase().includes("eventid")
    ) ?? next.eventId

    //
    // referenceId (UUID)
    //
    next.referenceId = filters.find(f =>
        /^[0-9a-fA-F-]{36}$/.test(f)
    )

    //
    // from:2024-01-01
    //
    const fromChip = filters.find(f => f.startsWith("from:"))
    if (fromChip) next.from = fromChip.substring(5)

    //
    // to:2024-01-31
    //
    const toChip = filters.find(f => f.startsWith("to:"))
    if (toChip) next.to = toChip.substring(3)

    //
    // NEW: eventTypes
    // 1) "StartProcessingEvent"
    // 2) "event:StartProcessingEvent"
    //
    const directEventTypes = filters.filter(f =>
        /^[A-Za-z0-9_]+Event$/.test(f)
    )

    const prefixedEventTypes = filters
        .filter(f => f.startsWith("event:"))
        .map(f => f.substring(6))
        .filter(f => /^[A-Za-z0-9_]+Event$/.test(f))

    const allEventTypes = [...directEventTypes, ...prefixedEventTypes]

    next.eventTypes = allEventTypes.length > 0 ? allEventTypes : []

    return next
}
