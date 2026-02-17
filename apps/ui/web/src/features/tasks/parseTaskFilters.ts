import type { TaskQuery } from "../../types/webTypes"
import { taskFilterSchema } from "./taskFilterSchema"

export function parseTaskFilters(filters: string[], base: TaskQuery): TaskQuery {
    const next = { ...base }

    next.status = filters.filter(f =>
        taskFilterSchema.status.map(s => s.toLowerCase()).includes(f.toLowerCase())
    )

    next.claimed = filters.includes("claimed")
        ? true
        : filters.includes("!claimed")
            ? false
            : undefined

    next.consumed = filters.includes("consumed")
        ? true
        : filters.includes("!consumed")
            ? false
            : undefined

    next.referenceId = filters.find(f => /^[0-9a-fA-F-]{36}$/.test(f))

    const keyChips = filters.filter(f => f.startsWith("key:"))

    if (keyChips.length > 0) {
        next.key = keyChips.map(k => k.substring(4))
    } else {
        next.key = undefined
    }


    return next
}
