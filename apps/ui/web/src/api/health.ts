import type { CoordinatorHealth } from "../types/backendTypes";
import { apiGet } from "./client";

export function getCoordinatorHealth() {
    return apiGet<CoordinatorHealth>("/health")
}