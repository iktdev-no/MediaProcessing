import type { SSEMessage } from "../types/SSEMessage"

export async function apiGet<T>(
    path: string,
    opts?: {
        onError?: (status: number, body: any) => void
    }
): Promise<T> {
    const res = await fetch(`/api${path}`, {
        headers: {
            Accept: "application/json"
        }
    })

    if (!res.ok) {
        const status = res.status
        let body: any = null

        try {
            body = await res.json()
        } catch {
            body = await res.text().catch(() => null)
        }

        // If user provided custom error handler → use it
        if (opts?.onError) {
            opts.onError(status, body)
            // Return a never-resolving promise so caller doesn't continue
            return Promise.reject({ status, body })
        }

        // Default behavior: throw a normal error
        const error: any = new Error(`GET ${path} failed with ${status}`)
        error.status = status
        error.body = body
        throw error
    }

    return res.json()
}


export async function apiPost<TRequest, TResponse>(path: string, body: TRequest): Promise<TResponse> {
    const res = await fetch(`/api${path}`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Accept": "*/*" // ← viktig: ikke tving JSON
        },
        body: JSON.stringify(body)
    })

    if (!res.ok) {
        // prøv å lese tekst hvis mulig
        const text = await res.text().catch(() => null)
        throw new Error(text || `POST ${path} failed with ${res.status}`)
    }

    // sjekk content-type
    const contentType = res.headers.get("content-type") ?? ""

    if (contentType.includes("application/json")) {
        return res.json()
    }

    // hvis det ikke er JSON → returner tekst
    const text = await res.text()
    return text as unknown as TResponse
}

export function apiSse(
    onEvent: (eventName: string, data: any) => void,
    onError?: (err: any) => void
): EventSource {
    const es = new EventSource("/api/sse") // hardkodet

    es.onmessage = (event) => {
        const message: SSEMessage = JSON.parse(event.data)
        onEvent(message.name, message.data)
    }

    es.onerror = (err) => {
        if (onError) onError(err)
    }

    return es
}


export function buildQuery(params: Record<string, any>): string {
    const search = new URLSearchParams()

    for (const [key, value] of Object.entries(params)) {
        if (value === undefined || value === null) continue

        if (Array.isArray(value)) {
            value.forEach(v => search.append(key, String(v)))
        } else {
            search.append(key, String(value))
        }
    }

    return search.toString()
}
