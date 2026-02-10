import type { SequenceSummary } from "../types/transfer-model"
import { apiGet, apiPost } from "./client"

export function getActiveSequences() {
    return apiGet<SequenceSummary[]>("/sequences/active")
}

export function getRecentSequences(limit = 15) {
    return apiGet<SequenceSummary[]>(`/sequences/recent?limit=${limit}`)
}

export async function continueSequence(referenceId: string) {
    try {
        const res = await apiPost(`/sequences/${referenceId}/continue`, {})

        // Hvis backend returnerer 200 OK → ferdig
        return

    } catch (err: any) {
        console.log(err)
        // Hvis backend returnerer 4xx/5xx → kast feilen videre
        throw new Error(err?.response?.data ?? "Unknown error")
    }
}
