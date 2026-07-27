import type { Progress } from "../../types/transfer-model";
import type {
  IgnoredTaskResponse,
  PagedUiTask,
  ResetTaskResponse,
  TaskQuery,
} from "../../types/webTypes";
import { apiGet, apiPatch, buildQuery } from "../client";

export function getTasks(query: TaskQuery) {
  const qs = buildQuery(query);
  return apiGet<PagedUiTask>(`/tasks?${qs}`);
}

export function resetFailedTask(
  taskId: string,
  force: boolean,
  opts?: { onError?: (status: number, body: any) => void },
) {
  if (force) {
    return apiGet<ResetTaskResponse>(`/tasks/taskid/${taskId}/reset/force`, opts);
  } else {
    return apiGet<ResetTaskResponse>(`/tasks/taskid/${taskId}/reset`, opts);
  }
}

export function getProgress() {
  return apiGet<Progress[]>("/tasks/progress");
}
1;
export function cancelTask(
  taskId: string,
  opts?: { onError?: (status: number, body: any) => void },
) {
  return apiGet<boolean>(`/tasks/taskid/${taskId}/cancel`, opts);
}

export function patchTaskOverride(
  taskId: string,
  overrideName: string,
  opts?: { onError?: (status: number, body: any) => void },
) {
  return apiPatch<string[], any>(
    `/tasks/taskid/${taskId}/override`,
    [overrideName],
    opts,
  );
}


export function patchTaskIgnore(taskId: string, opts?: { onError?: (status: number, body: any) => void }) {
  return apiPatch<null, IgnoredTaskResponse>(`/tasks/taskid/${taskId}/ignore`, null)
}