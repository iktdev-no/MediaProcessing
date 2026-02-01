import {
    Box,
    Paper,
    Typography
} from "@mui/material"

import BackHandIcon from '@mui/icons-material/BackHand'
import BlockIcon from "@mui/icons-material/Block"
import ErrorIcon from "@mui/icons-material/Error"
import HourglassEmptyIcon from "@mui/icons-material/HourglassEmpty"
import PlayArrowIcon from "@mui/icons-material/PlayArrow"
import ReportProblemIcon from "@mui/icons-material/ReportProblem"

import { useTheme } from "@mui/material"

function TaskCard({ label, value, color, icon }: any) {
    const theme = useTheme()
    const isZero = value === 0

    const bg = isZero ? theme.palette.grey[900] : color
    const textColor = theme.palette.getContrastText(bg)

    return (
        <Paper
            elevation={isZero ? 0 : 2}
            sx={{
                p: 2,
                borderRadius: 2,
                border: 1,
                borderColor: isZero ? "grey.700" : color,
                backgroundColor: bg,
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
                height: "100%",
            }}
        >
            <Box sx={{ display: "flex", alignItems: "center", gap: 1, }} >
                <Box sx={{ color: textColor, display: "flex", alignItems: "center" }}>
                    {icon}
                </Box>
                <Typography variant="body2" sx={{ color: textColor, fontWeight: 600 }}>
                    {label}
                </Typography>
            </Box>

            <Typography variant="h4" sx={{ color: textColor, fontWeight: 700, mt: 2 }}>
                {value}
            </Typography>
        </Paper>
    )
}



export function TaskOverview({ active, queued, stalled, abandoned, failed, onHold }: {
    active: number, queued: number, stalled: number, abandoned: number, failed: number, onHold: number
}) {
    const theme = useTheme()

    const cards = [
        { label: "Active", value: active, color: theme.palette.primary.main, icon: <PlayArrowIcon /> },
        { label: "On Hold", value: onHold, color: theme.palette.warning.main, icon: <BackHandIcon /> },
        { label: "Queued", value: queued, color: theme.palette.info.main, icon: <HourglassEmptyIcon /> },
        { label: "Stalled", value: stalled, color: theme.palette.warning.main, icon: <ReportProblemIcon /> },
        { label: "Abandoned", value: abandoned, color: theme.palette.error.main, icon: <BlockIcon /> },
        { label: "Failed", value: failed, color: theme.palette.error.main, icon: <ErrorIcon /> },
    ]


    return (
        <Box
            sx={{
                mt: 2,
                display: "grid",
                gap: 2,
                gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                alignItems: "stretch",
            }}
        >
            {cards.map(card => (
                <TaskCard key={card.label} {...card} />
            ))}
        </Box>
    )
}
