import {
    createContext,
    type PropsWithChildren,
    useContext,
    useEffect,
    useState
} from "react";
import { getProgress } from "../api/tasks"; // ← ny klientfunksjon
import { subscribe } from "../sse/eventBus";
import type { ProgressUpdate } from "../types/types";

type ProgressMap = Map<string, ProgressUpdate>

type ProgressContextType = {
    progress: ProgressMap
}

const ProgressContext = createContext<ProgressContextType>({
    progress: new Map()
})

export function useProgress() {
    return useContext(ProgressContext)
}

const STORAGE_KEY = "progress-cache-v1"

// Serialize Map → JSON
function serialize(map: ProgressMap): string {
    return JSON.stringify(Object.fromEntries(map))
}

// Deserialize JSON → Map
function deserialize(raw: string | null): ProgressMap {
    if (!raw) return new Map()
    try {
        const obj = JSON.parse(raw)
        return new Map(Object.entries(obj))
    } catch {
        return new Map()
    }
}

export function ProgressProvider({ children }: PropsWithChildren) {
    // 1) Hydrate from localStorage
    const [progress, setProgress] = useState<ProgressMap>(() => {
        return deserialize(localStorage.getItem(STORAGE_KEY))
    })

    // 2) Persist to localStorage whenever progress changes
    useEffect(() => {
        localStorage.setItem(STORAGE_KEY, serialize(progress))
    }, [progress])

    // 3) Fetch snapshot from backend (list → Map)
    useEffect(() => {
        getProgress()
            .then(list => {
                const map = new Map<string, ProgressUpdate>()
                for (const item of list) {
                    map.set(item.taskId, item)
                }
                setProgress(map)
            })
            .catch(err => {
                console.error("Failed to fetch progress snapshot", err)
            })
    }, [])

    // 4) Subscribe to SSE updates
    useEffect(() => {
        const unsubscribe = subscribe("progress", (data: ProgressUpdate) => {
            setProgress(prev => {
                const updated = new Map(prev)
                updated.set(data.taskId, data)
                return updated
            })
        })

        return () => unsubscribe()
    }, [])

    return (
        <ProgressContext.Provider value={{ progress }}>
            {children}
        </ProgressContext.Provider>
    )
}
