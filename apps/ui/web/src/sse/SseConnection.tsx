import { useEffect, useRef } from 'react';
import type { SseDispatcher } from './dispatcher';

export const useSseConnection = (dispatcher: SseDispatcher) => {
  const HEARTBEAT_TIMEOUT = 15000;
  // Bruk 'number' i stedet for NodeJS.Timeout
  const timerRef = useRef<number | null>(null);

  useEffect(() => {
    let es: EventSource | null = null;

    const resetWatchdog = () => {
      if (timerRef.current) clearTimeout(timerRef.current);

      timerRef.current = window.setTimeout(() => {
        console.warn("Watchdog: Server ping timeout. Reconnecting...");
        es?.close();
        connect();
      }, HEARTBEAT_TIMEOUT);
    };

    const connect = () => {
      es?.close();
      es = new EventSource('/api/sse');
      dispatcher.dispatch({ type: "sse-connecting" });

      es.onopen = () => {
        console.log("SSE: Connected");
        dispatcher.dispatch({ type: "sse-online" });
        resetWatchdog();
      };

      es.onmessage = (e) => {
        try {
          const event = JSON.parse(e.data);
          if (event.type === 'ping') {
            resetWatchdog();
          }
          dispatcher.dispatch(event);
        } catch {
          dispatcher.dispatch({ type: 'custom', payload: e.data });
        }
      };

      es.onerror = () => {
        dispatcher.dispatch({ type: "sse-offline" });
        if (timerRef.current) clearTimeout(timerRef.current);

        // Vent 3 sekunder før vi prøver igjen
        window.setTimeout(connect, 3000);
      };
    };

    connect();

    return () => {
      es?.close();
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [dispatcher]);
};