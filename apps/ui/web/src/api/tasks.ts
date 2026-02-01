import type { PagedUiTask, ResetTaskResponse, TaskQuery } from "../types/backendTypes";
import { apiGet, buildQuery } from "./client";

export function getTasks(query: TaskQuery) {
    const qs = buildQuery(query)
    return apiGet<PagedUiTask>(`/tasks?${qs}`)
}

export function resetFailedTask(
    taskId: string,
    force: boolean,
    opts?: { onError?: (status: number, body: any) => void }
) {
    if (force) {
        return apiGet<ResetTaskResponse>(`/tasks/${taskId}/reset/force`, opts)
    } else {
        return apiGet<ResetTaskResponse>(`/tasks/${taskId}/reset`, opts)
    }
}
