import type { LifecycleNode, Sequence, SequenceSummary } from "../../types/types";
import { apiGet, apiPost } from "../client";

export function getActiveSequences() {
  return apiGet<Sequence[]>("/sequences/active");
}

export function getRecentSequences(limit = 15) {
  return apiGet<Sequence[]>(`/sequences/recent?limit=${limit}`);
}

export function getSequence(referenceId: string) {
  return apiGet<Array<LifecycleNode>>(`/sequences/${referenceId}`)
}

export function getSequenceInfo(referenceId: string) {
  return apiGet<SequenceSummary>(`/sequences/${referenceId}/info`)
}

export async function continueSequence(referenceId: string) {
  try {
    const res = await apiPost(`/sequences/${referenceId}/continue`, {});

    // Hvis backend returnerer 200 OK → ferdig
    return;
  } catch (err: any) {
    console.log(err);
    // Hvis backend returnerer 4xx/5xx → kast feilen videre
    throw new Error(err?.response?.data ?? "Unknown error");
  }
}
