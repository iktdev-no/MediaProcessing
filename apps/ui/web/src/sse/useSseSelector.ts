import { useEffect, useState } from 'react';
import { useSseDispatcher } from './SseProvider';
import type { SseState } from './state';

export function useSseSelector<T>(selector: (state: SseState) => T): T {
    const dispatcher = useSseDispatcher();
    const [value, setValue] = useState(() => selector(dispatcher.getState()));

    useEffect(() => {
        const unsubscribe = dispatcher.subscribe((state) => {
            setValue(selector(state));
        });

        return () => {
            unsubscribe(); // ignorer returverdien
        };
    }, [dispatcher, selector]);

    return value;
}
