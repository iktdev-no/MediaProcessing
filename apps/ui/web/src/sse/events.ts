import type { Progress, ProgressRef, SystemStatus } from "../types/types";

export type SseEvent =
  | { type: 'ping'; timestamp: number }
  | { type: 'progress', progress: ProgressRef }
  | { type: "health-status", systemHealth: SystemStatus }
  | { type: "sse-online"; }
  | { type: "sse-connecting"; }
  | { type: "sse-offline"; }
  | { type: 'custom'; payload: unknown }

  ;
