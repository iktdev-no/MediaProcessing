import ChevronRightIcon from "@mui/icons-material/ChevronRight"
import ExpandMoreIcon from "@mui/icons-material/ExpandMore"
import { Box, IconButton, Typography } from "@mui/material"
import { useState } from "react"
import { JSON_VIEWER_CONFIG as C } from "../theme/theme"
import { deepUnwrapJson } from "../util"

export interface JsonViewerProps {
    value: unknown
    level?: number
}

export function JsonViewer({ value, level = 0 }: JsonViewerProps) {
    const unwrapped = deepUnwrapJson(value)
    const indent = level * C.indentPx

    const renderPrimitive = (v: unknown) => {
        if (v === null)
            return <Typography color={C.colors.null}>null</Typography>

        if (typeof v === "number")
            return <Typography color={C.colors.number}>{v}</Typography>

        if (typeof v === "boolean")
            return <Typography color={C.colors.boolean}>{String(v)}</Typography>

        if (typeof v === "string")
            return (
                <Typography color={C.colors.string} sx={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}>
                    "{v}"
                </Typography>
            )

        return (
            <Typography color={C.colors.unknown}>
                {String(v)}
            </Typography>
        )
    }

    // Primitive
    if (
        unwrapped === null ||
        typeof unwrapped === "number" ||
        typeof unwrapped === "boolean" ||
        typeof unwrapped === "string"
    ) {
        return (
            <Box sx={{ ml: indent }}>
                {renderPrimitive(unwrapped)}
            </Box>
        )
    }

    // Array
    if (Array.isArray(unwrapped)) {
        const [open, setOpen] = useState(true)

        return (
            <Box sx={{ ml: indent }}>
                <Box display="flex" alignItems="center">
                    <IconButton size="small" onClick={() => setOpen(!open)}>
                        {open ? <ExpandMoreIcon /> : <ChevronRightIcon />}
                    </IconButton>
                    <Typography color={C.colors.bracket}>[</Typography>
                </Box>

                {open &&
                    unwrapped.map((item, i) => (
                        <JsonViewer key={i} value={item} level={level + 1} />
                    ))}

                <Typography sx={{ ml: indent }} color={C.colors.bracket}>]</Typography>
            </Box>
        )
    }

    // Object
    if (typeof unwrapped === "object") {
        const [open, setOpen] = useState(true)
        const entries = Object.entries(unwrapped as Record<string, unknown>)

        return (
            <Box sx={{ ml: indent }}>
                <Box display="flex" alignItems="center">
                    <IconButton size="small" onClick={() => setOpen(!open)}>
                        {open ? <ExpandMoreIcon /> : <ChevronRightIcon />}
                    </IconButton>
                    <Typography color={C.colors.bracket}>{"{"}</Typography>
                </Box>

                {open &&
                    entries.map(([key, val]) => {
                        const isPrimitive =
                            val === null ||
                            typeof val === "string" ||
                            typeof val === "number" ||
                            typeof val === "boolean"

                        return (
                            <Box key={key} sx={{ ml: C.indentPx, display: "flex" }}>
                                <Typography
                                    component="span"
                                    sx={{ color: C.colors.key, mr: 1 }}
                                >
                                    {key}:
                                </Typography>

                                {isPrimitive ? (
                                    <Box sx={{ flex: 1 }}>{renderPrimitive(val)}</Box>
                                ) : (
                                    <Box sx={{ flex: 1 }}>
                                        <JsonViewer value={val} level={level + 1} />
                                    </Box>
                                )}
                            </Box>
                        )
                    })}

                <Typography sx={{ ml: indent }} color={C.colors.bracket}>{"}"}</Typography>
            </Box>
        )
    }

    return null
}
