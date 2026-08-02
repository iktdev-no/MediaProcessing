import type { SseEvent } from './events';
import { sseReducer } from './reducer';
import type { SseState } from './state';

export class SseDispatcher {
  private state: SseState;
  private listeners: Set<(state: SseState) => void> = new Set();

  constructor(initial: SseState) {
    this.state = initial;
  }

  dispatch(event: SseEvent) {
    this.state = sseReducer(this.state, event);
    this.listeners.forEach((l) => l(this.state));
  }

  subscribe(listener: (state: SseState) => void) {
    this.listeners.add(listener);
    listener(this.state);
    return () => this.listeners.delete(listener);
  }

  getState() {
    return this.state;
  }
}
