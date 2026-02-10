import type { CoordinatorHealth } from "../types/transfer-model";
import { apiGet } from "./client";

export function getCoordinatorHealth() {
    return apiGet<CoordinatorHealth>("/health")
}