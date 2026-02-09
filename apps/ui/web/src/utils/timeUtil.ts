export function formatDuration(seconds: number | null): string {
    if (seconds == null || seconds <= 0) return "0s";

    const d = Math.floor(seconds / 86400);
    const h = Math.floor((seconds % 86400) / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);

    const parts: string[] = [];

    if (d > 0) {
        parts.push(`${d}d`);
        if (h > 0) parts.push(`${h}t`);
        return parts.slice(0, 2).join(" ");
    }

    if (h > 0) {
        parts.push(`${h}t`);
        if (m > 0) parts.push(`${m}m`);
        return parts.slice(0, 2).join(" ");
    }

    if (m > 0) {
        parts.push(`${m}m`);
        if (s > 0) parts.push(`${s}s`);
        return parts.slice(0, 2).join(" ");
    }

    return `${s}s`;
}
