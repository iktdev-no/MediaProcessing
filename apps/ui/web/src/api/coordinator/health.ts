import type { SystemHealth } from "../../types/types";
import { apiGet } from "../client";

export function getSystemHealth() {
  return apiGet<SystemHealth>("/health");
}
