import type { Progress, ProgressRef, SystemHealth, SystemStatus } from "../types/types";

export type SSEConnectionState = "Connected" | "Connecting" | "Disconnected"

export interface SseState {
  lastPing?: number;
  progress: Record<string, ProgressRef>;
  systemHealth?: SystemStatus,
  sseState: SSEConnectionState
}

export const initialSseState: SseState = {
  progress: {},
  sseState: "Disconnected"
};
