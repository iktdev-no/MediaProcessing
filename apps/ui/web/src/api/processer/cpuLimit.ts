import type { CPULimit } from "../../types/transfer-model";
import { apiGet, apiPost } from "../client";

export function getCpuLimit() {
  return apiGet<CPULimit>("/processer/cpu-limit");
}

export function setCpuLimit(limit: CPULimit) {
  return apiPost<CPULimit, void>("/processer/cpu-limit", limit);
}
