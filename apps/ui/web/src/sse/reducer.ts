import type { SseEvent } from './events';
import type { SseState } from './state';

export function sseReducer(state: SseState, event: SseEvent): SseState {
  switch (event.type) {
    case 'ping':
      return { ...state, lastPing: event.timestamp };

    case 'progress':
      return {
        ...state,
        progress: {
          ...state.progress,
          [event.progress.taskId]: event.progress
        }
      };

    case 'health-status':
      return {
        ...state,
        systemHealth: event.systemHealth
      }

    case 'sse-connecting':
      return {
        ...state,
        sseState: "Connecting"
      }

    case 'sse-online':
      return {
        ...state,
        sseState: "Connected"
      }

    case 'sse-offline':
      return {
        ...state,
        sseState: "Disconnected"
      }

    default:
      console.log("Ingen tok seg av ", event)
      return state;
  }
}
