import type { EventQuery, PagedUiEvent, UiEvent } from "../types/backendTypes";
import { apiGet, buildQuery } from "./client";

export function getEvents(query: EventQuery) {
    const qs = buildQuery(query);
    return apiGet<PagedUiEvent>(`/events?${qs}`)
}

export function getEffectiveEventsHistory(referenceId: string) {
    return apiGet<UiEvent[]>(`/events/history/${referenceId}/effective`)
}