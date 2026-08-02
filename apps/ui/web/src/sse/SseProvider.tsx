import { createContext, useContext, useEffect, useMemo } from 'react';
import { SseDispatcher } from './dispatcher';
import { initialSseState } from './state';
import type { SseEvent } from './events';
import { useSseConnection } from './SseConnection';


const SseContext = createContext<SseDispatcher | null>(null);

export const useSseDispatcher = () => useContext(SseContext)!;

export const SseProvider = ({ children }: { children: React.ReactNode }) => {
    const dispatcher = useMemo(() => new SseDispatcher(initialSseState), []);

    // Vi "hooker" oss inn i tilkoblingen her
    useSseConnection(dispatcher);

    return (
        <SseContext.Provider value={dispatcher}>
            {children}
        </SseContext.Provider>
    );
};