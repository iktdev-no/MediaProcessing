
import CheckIcon from "@mui/icons-material/Check"
import CloseIcon from "@mui/icons-material/Close"
import DoNotDisturbIcon from '@mui/icons-material/DoNotDisturb'
import { Box, Button, Chip, Paper, Typography } from "@mui/material"
import { useState } from "react"
import type { UiEvent, UiTask } from "../../types/types"
import { DetailsButton } from "../DetailsButton"
import { useSseSelector } from "../../sse/useSseSelector"
import { TaskStatusIcon } from "../task/TaskStatus"
import { TaskProgress } from "../task/TaskProgress"
import { TaskDetailsDialog } from "../task/TaskDetailsDialog"
import { EventDialog } from "../event/EventDialog"

export interface EventCardProps {
    event: UiEvent
}

export function SequenceEventCard({ event }: EventCardProps) {
    const [open, setOpen] = useState(false)




    const updatedAt = event.persistedAt
    const formatted = new Intl.DateTimeFormat("no-NO", {
        dateStyle: "short",
        timeStyle: "medium"
    }).format(new Date(updatedAt))

    return (
        <>
            <Paper sx={{ p: 1.25, display: "flex", flexDirection: "column", gap: 0.5 }}>

                {/* HEADER GRID: venstre + høyre */}
                <Box
                    sx={{
                        display: "grid",
                        gridTemplateColumns: "1fr auto", // venstre | høyre
                        columnGap: 2,
                        width: "100%",
                        alignItems: "stretch"
                    }}
                >

                    {/* VENSTRE SIDE (2 rader) */}
                    <Box
                        sx={{
                            display: "grid",
                            gridTemplateRows: "auto auto",
                            rowGap: 1
                        }}
                    >
                        {/* Rad 1: ikon, navn, chips, timestamp */}
                        <Box
                            sx={{
                                display: "grid",
                                gridTemplateColumns: "auto 1fr 8fr 1fr",
                                alignItems: "center",
                                columnGap: 2
                            }}
                        >

                            <Typography variant="body2" sx={{ fontWeight: 600, whiteSpace: "nowrap" }}>
                                {event.event}
                            </Typography>


                            <Typography variant="caption" sx={{ color: "text.secondary", whiteSpace: "nowrap" }}>
                                {formatted}
                            </Typography>
                        </Box>

                        {/* Rad 2: IDs */}
                        <Typography
                            variant="caption"
                            sx={{
                                color: "text.secondary",
                                opacity: 0.8,
                                display: "flex",
                                gap: 0.5
                            }}
                        >
                            {event.eventId}
                        </Typography>
                    </Box>

                    {/* HØYRE SIDE: knappene (vertikal stack) */}
                    <Box
                        sx={{
                            display: "flex",
                            flexDirection: "row",
                            justifyContent: "space-between",
                            alignItems: "center",
                            gap: 1,
                            height: "100%"
                        }}
                    >

                        <DetailsButton onClick={() => setOpen(true)} />
                    </Box>
                </Box>

            </Paper>


            {/* Popup */}
            <EventDialog open={open} onClose={() => setOpen(false)} event={event} />

        </>
    )
}
