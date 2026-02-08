export const normalDate = new Intl.DateTimeFormat("no-NO", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
})


export function parseDurationMs(iso: string | number | undefined | null): number {
    if (iso == null) return 0;

    // If backend sends seconds as number
    if (typeof iso === "number") {
        return iso * 1000;
    }

    // If backend sends ISO-8601 duration
    const match = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);
    if (!match) return 0;

    const hours = match[1] ? Number(match[1]) : 0;
    const minutes = match[2] ? Number(match[2]) : 0;
    const seconds = match[3] ? Number(match[3]) : 0;

    return (hours * 3600 + minutes * 60 + seconds) * 1000;
}


export function formatDurationMs(ms: number): string {
    const minutes = Math.floor(ms / 60000)
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60

    if (hours > 0) return `${hours} t ${mins} min`
    return `${minutes} min`
}



export function deepUnwrapJson(value: unknown): unknown {
    // Hvis det ikke er en string → returner som det er
    if (typeof value !== "string") {
        return value
    }

    let current: unknown = value

    while (typeof current === "string") {
        try {
            const parsed = JSON.parse(current)
            current = parsed
        } catch {
            break
        }
    }

    return current
}


function hashStringToInt(str: string): number {
    let hash = 0
    for (let i = 0; i < str.length; i++) {
        hash = (hash << 5) - hash + str.charCodeAt(i)
        hash |= 0
    }
    return Math.abs(hash)
}

export function colorFromUuid(uuid: string): string {
    const hash = hashStringToInt(uuid)

    // Golden angle for best visual separation
    const hue = (hash * 137.508) % 360

    // Neon mode for dark UI
    const saturation = 90   // høy saturation = neon
    const lightness = 70    // lys nok til å poppe på mørk bakgrunn

    return `hsl(${hue}, ${saturation}%, ${lightness}%)`
}
