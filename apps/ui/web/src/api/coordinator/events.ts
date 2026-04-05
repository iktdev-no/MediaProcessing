import type { DeleteResult, LineageNode } from "../../types/transfer-model";
import type { UiEvent } from "../../types/types";
import type { EventQuery, PagedUiEvent } from "../../types/webTypes";
import { apiDelete, apiGet, buildQuery } from "../client";

export function getEvents(query: EventQuery) {
  const qs = buildQuery(query);
  return apiGet<PagedUiEvent>(`/events?${qs}`);
}

export function getEffectiveEventsHistory(referenceId: string) {
  return apiGet<UiEvent[]>(`/events/history/${referenceId}/effective`);
}

export function getEventsLineage(referenceId: string) {
  return apiGet<LineageNode[]>(`/events/${referenceId}/lineage`);
}

export function deleteEvent(referenceId: string, eventId: string) {
  return apiDelete<DeleteResult>(`/events/delete/${referenceId}/${eventId}`);
}
