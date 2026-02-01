import { useEffect, useRef } from "react"

export function useAutoRefresh(callback: () => void, intervalMs: number | null) {
    const savedCallback = useRef(callback)

    // Hold callback oppdatert
    useEffect(() => {
        savedCallback.current = callback
    }, [callback])

    useEffect(() => {
        // Hvis intervallet er null → ingen auto-refresh
        if (intervalMs === null) {
            return
        }

        const id = setInterval(() => savedCallback.current(), intervalMs)
        return () => clearInterval(id)
    }, [intervalMs])
}
