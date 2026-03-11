import { Box, Chip, Paper, TextField } from "@mui/material"
import { useMemo, useState } from "react"

export interface FilterChipsProps {
    value: string[]
    onChange: (filters: string[]) => void

    onBeforeAdd?: (token: string, current: string[]) => string[] | null
    onBeforeRemove?: (token: string, current: string[]) => string[] | null

    suggestions?: string[]
    keySuggestions?: string[]
    keyLabel?: string
    placeholder?: string
}

export function FilterChips({
    value,
    onChange,
    onBeforeAdd,
    onBeforeRemove,
    suggestions = [],
    keySuggestions = [],
    keyLabel = "Key",
    placeholder = "Legg til filter…"
}: FilterChipsProps) {

    const [input, setInput] = useState("")

    const allSuggestions = useMemo(() => {
        const lower = input.toLowerCase()
        const base = suggestions.filter(s => s.toLowerCase().includes(lower))
        const keys = keySuggestions
            .filter(k => k.toLowerCase().includes(lower))
            .map(k => `key:${k}`)
        return [...base, ...keys]
    }, [input, suggestions, keySuggestions])

    const addFilter = (token: string) => {
        let next = [...value]

        if (onBeforeAdd) {
            const result = onBeforeAdd(token, next)
            if (result === null) {
                setInput("")
                return
            }
            next = result
        }

        if (!next.includes(token)) {
            next.push(token)
        }

        onChange(next)
        setInput("")
    }

    const removeFilter = (token: string) => {
        let next = [...value]

        if (onBeforeRemove) {
            const result = onBeforeRemove(token, next)
            if (result === null) return
            next = result
        } else {
            next = next.filter(f => f !== token)
        }

        onChange(next)
    }

    const prettyLabel = (token: string) => {
        // key:value
        if (token.includes(":")) {
            const [key, value] = token.split(":")
            if (value) {
                return `${key}: ${value}`
            }
            return `${key}:`
        }

        // event name (fra suggestions)
        if (suggestions.includes(token)) {
            return `Event: ${token}`
        }

        // fallback
        return token
    }


    return (
        <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
            <TextField
                size="small"
                label={placeholder}
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => {
                    if (e.key === "Enter" && input.trim()) {
                        addFilter(input.trim())
                    }
                }}
            />

            {/* Forslag */}
            {input && allSuggestions.length > 0 && (
                <Paper
                    sx={{
                        p: 1,
                        display: "flex",
                        gap: 1,
                        flexWrap: "wrap",
                        background: "rgba(0,0,0,0.04)"
                    }}
                >
                    {allSuggestions.map(s => (
                        <Chip
                            key={s}
                            label={prettyLabel(s)}
                            variant="outlined"
                            onClick={() => addFilter(s)}
                        />
                    ))}
                </Paper>
            )}

            {/* Aktive chips */}
            <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                {value.map(f => (
                    <Chip
                        key={f}
                        label={prettyLabel(f)}
                        onDelete={() => removeFilter(f)}
                    />
                ))}
            </Box>
        </Box>
    )
}
