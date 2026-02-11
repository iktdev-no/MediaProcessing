import type { EventQuery, PagedUiEvent } from "../types/backendTypes";
import type { DeleteResult } from "../types/transfer-model";
import type { UiEvent } from "../types/types";
import { apiDelete, apiGet, buildQuery } from "./client";

export function getEvents(query: EventQuery) {
    const qs = buildQuery(query);
    return apiGet<PagedUiEvent>(`/events?${qs}`)
}

export function getEffectiveEventsHistory(referenceId: string) {
    return apiGet<UiEvent[]>(`/events/history/${referenceId}/effective`)
}


export function deleteEvent(referenceId: string, eventId: string) {
    return apiDelete<DeleteResult>(`/events/delete/${referenceId}/${eventId}`)
}
