import CheckIcon from "@mui/icons-material/Check"
import CloseIcon from "@mui/icons-material/Close"
import DoNotDisturbIcon from '@mui/icons-material/DoNotDisturb'
import { Box, Button, Chip, Paper, Typography } from "@mui/material"
import { useState } from "react"
import { useProgress } from "../../context/ProgressProvider"
import type { UiTask } from "../../types/types"
import { DetailsButton } from "../DetailsButton"
import { TaskDetailsDialog } from "./TaskDetailsDialog"
import { TaskProgress } from "./TaskProgress"
import { TaskStatusIcon } from "./TaskStatus"

export interface TaskCardProps {
    task: UiTask
    show: "taskId" | "referenceId"
    onCopy: () => void
    onReferenceIdClicked: (referenceId: string) => void
    onCanceltask: (taskId: string) => void
}

export function TaskCard({ task, show, onCopy, onReferenceIdClicked, onCanceltask }: TaskCardProps) {
    const live = useProgress().progress.get(task.taskId)

    const [open, setOpen] = useState(false)




    const updatedAt = task.lastCheckIn ?? task.persistedAt
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
                            <TaskStatusIcon status={task.status} />

                            <Typography variant="body2" sx={{ fontWeight: 600, whiteSpace: "nowrap" }}>
                                {task.task}
                            </Typography>

                            <Box display="flex" gap={1}>
                                <Chip
                                    label="Claimed"
                                    color={task.claimed ? "success" : "default"}
                                    size="small"
                                    icon={task.claimed ? <CheckIcon /> : <CloseIcon />}
                                />
                                <Chip
                                    label="Consumed"
                                    color={task.consumed ? "success" : "default"}
                                    size="small"
                                    icon={task.consumed ? <CheckIcon /> : <CloseIcon />}
                                />
                            </Box>

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
                            <Box
                                component="span"
                                onClick={() => onReferenceIdClicked(task.referenceId)}
                                sx={{
                                    cursor: "pointer",
                                    px: 0.3,
                                    borderRadius: 0.5,
                                    "&:hover": {
                                        backgroundColor: "action.hover",
                                        color: "text.primary"
                                    }
                                }}
                            >
                                {task.referenceId}
                            </Box>

                            • {task.taskId}
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
                        {["Pending", "InProgress"].includes(task.status) && (
                            <Button size="large" variant="outlined" color="error" sx={{
                                paddingX: 0,
                            }}
                                onClick={() => onCanceltask(task.taskId)}>
                                <DoNotDisturbIcon />
                            </Button>
                        )}

                        <DetailsButton onClick={() => setOpen(true)} />
                    </Box>
                </Box>

                {/* Progress section */}
                <TaskProgress task={task} progressUpdate={live} />

            </Paper>


            {/* Popup */}
            <TaskDetailsDialog open={open} onClose={() => setOpen(false)} task={task} onCopy={onCopy} />

        </>
    )
}
