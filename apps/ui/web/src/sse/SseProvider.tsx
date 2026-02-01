import { type PropsWithChildren, useEffect } from "react"
import { apiSse } from "../api/client"
import { dispatch } from "../sse/eventBus"

// Eksporter en global forceReconnect som HealthProvider kan bruke
let forceReconnectCallback: (() => void) | null = null
export function forceReconnect() {
    if (forceReconnectCallback) {
        forceReconnectCallback()
    }
}

export function SseProvider({ children }: PropsWithChildren) {
    useEffect(() => {
        let es: EventSource | null = null
        let reconnectTimer: ReturnType<typeof setTimeout> | null = null
        const reconnectDelay = 3000

        const connect = () => {
            // Lukk gammel forbindelse hvis den finnes
            if (es) es.close()

            es = apiSse(
                (eventName, data) => {
                    //console.log(`SseProvider: Received event "${eventName}"`, data)
                    dispatch(eventName, data)
                },
                () => {
                    // SSE feilet → planlegg reconnect
                    if (!reconnectTimer) {
                        reconnectTimer = setTimeout(() => {
                            reconnectTimer = null
                            connect()
                        }, reconnectDelay)
                    }
                }
            )
        }

        // Gjør reconnect tilgjengelig for HealthProvider
        forceReconnectCallback = () => {
            console.log("SseProvider: forceReconnect() called")
            if (es) es.close()
            connect()
        }

        connect()

        return () => {
            if (es) es.close()
            if (reconnectTimer) clearTimeout(reconnectTimer)
            forceReconnectCallback = null
        }
    }, [])

    return <>{children}</>
}
