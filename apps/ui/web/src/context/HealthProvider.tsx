import {
    createContext,
    type PropsWithChildren,
    useContext,
    useEffect,
    useRef,
    useState
} from "react"
import { apiGet } from "../api/client"
import { subscribe } from "../sse/eventBus"
import { forceReconnect } from "../sse/SseProvider"
import type { SystemStatus } from "../types/types"

export type HealthStatus = "healthy" | "unhealthy" | "reconnecting"

type HealthContextValue = {
    status: HealthStatus
    raw: SystemStatus | null
    backend: BackendUiStatus | null
}

type BackendUiStatus = {
    sseOk: boolean
    restOk: boolean
}

const HealthContext = createContext<HealthContextValue>({
    status: "reconnecting",
    raw: null,
    backend: { sseOk: false, restOk: false },
})

export function useHealth() {
    return useContext(HealthContext)
}

function computeHealth(status: SystemStatus | null): HealthStatus {
    if (!status) return "unhealthy"

    const { timestamp, interval, ...rest } = status
    return Object.values(rest).every(v => v === true)
        ? "healthy"
        : "unhealthy"
}

export function HealthProvider({ children }: PropsWithChildren) {
    const [raw, setRaw] = useState<SystemStatus | null>(null)
    const [status, setStatus] = useState<HealthStatus>("reconnecting")
    const [backend, setBackend] = useState<BackendUiStatus>({
        sseOk: false,
        restOk: false,
    })

    const lastTimestamp = useRef<number>(0)
    const expectedInterval = useRef<number>(0)

    useEffect(() => {
        // 1. Initial REST fetch
        apiGet<SystemStatus>("/status")
            .then((data) => {
                setRaw(data)
                setStatus(computeHealth(data))
                lastTimestamp.current = data.timestamp
                expectedInterval.current = data.interval

                setBackend(prev => ({
                    ...prev,
                    restOk: true,
                }))
            })
            .catch(() => {
                setStatus("reconnecting")
                setBackend(prev => ({
                    ...prev,
                    restOk: false,
                }))
            })

        // 2. Subscribe to SSE
        const unsubscribe = subscribe("healthStatus", (data: SystemStatus) => {
            setRaw(data)
            setStatus(computeHealth(data))
            lastTimestamp.current = data.timestamp
            expectedInterval.current = data.interval

            setBackend(prev => ({
                ...prev,
                sseOk: true,
            }))
        })

        // 3. Watchdog: check if SSE is late
        const watchdog = setInterval(() => {
            if (!lastTimestamp.current || !expectedInterval.current) return

            const now = Date.now()
            const delta = now - lastTimestamp.current

            if (delta > expectedInterval.current * 5) {
                apiGet<SystemStatus>("/status")
                    .then((data) => {
                        setRaw(data)
                        setStatus(computeHealth(data))
                        lastTimestamp.current = data.timestamp
                        expectedInterval.current = data.interval

                        setBackend(prev => ({
                            ...prev,
                            restOk: true,
                        }))

                        forceReconnect()
                    })
                    .catch(() => {
                        setStatus("unhealthy")
                        setBackend({
                            sseOk: false,
                            restOk: false,
                        })
                    })
            }
        }, 1000)

        return () => {
            unsubscribe()
            clearInterval(watchdog)
        }
    }, [])

    return (
        <HealthContext.Provider value={{ status, raw, backend }}>
            {children}
        </HealthContext.Provider>
    )
}
