import CheckIcon from "@mui/icons-material/Check"
import CloseIcon from "@mui/icons-material/Close"
import { Box, Chip, Paper, Typography } from "@mui/material"
import { useState } from "react"
import type { UiTask } from "../../types/backendTypes"
import { DetailsButton } from "../DetailsButton"
import { TaskDetailsDialog } from "./TaskDetailsDialog"
import { TaskStatusIcon } from "./TaskStatus"

export interface TaskCardProps {
    task: UiTask
    show: "taskId" | "referenceId"
    onCopy: () => void
    onReferenceIdClicked: (referenceId: string) => void
}

export function TaskCard({ task, show, onCopy, onReferenceIdClicked }: TaskCardProps) {
    const [open, setOpen] = useState(false)
    const updatedAt = task.lastCheckIn ?? task.persistedAt
    const formatted = new Intl.DateTimeFormat("no-NO", {
        dateStyle: "short",
        timeStyle: "medium"
    }).format(new Date(updatedAt))

    return (
        <>
            <Paper sx={{ p: 1.25, display: "flex", flexDirection: "column", gap: 0.5 }}>

                {/* Row 1: 5-column grid */}
                <Box
                    sx={{
                        display: "grid",
                        gridTemplateColumns: "auto 1fr 8fr auto auto",
                        alignItems: "center",
                        columnGap: 2,
                        width: "100%"
                    }}
                >
                    {/* Col 1: Icon */}
                    <TaskStatusIcon status={task.status} />

                    {/* Col 2: Event name */}
                    <Typography variant="body2" sx={{ fontWeight: 600, whiteSpace: "nowrap" }}>
                        {task.task}
                    </Typography>

                    {/* Col 3: Chips */}
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

                    {/* Col 4: Timestamp */}
                    <Typography variant="caption" sx={{ color: "text.secondary", whiteSpace: "nowrap" }}>
                        {formatted}
                    </Typography>

                    {/* Col 5: Details button */}
                    <DetailsButton onClick={() => setOpen(true)} />
                </Box>

                {/* Row 2: IDs */}
                <Typography
                    variant="caption"
                    sx={{
                        color: "text.secondary",
                        opacity: 0.8,
                        mt: 0.25,
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
                            transition: "background-color 0.15s ease",
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


            </Paper>

            {/* Popup */}
            <TaskDetailsDialog open={open} onClose={() => setOpen(false)} task={task} onCopy={onCopy} />
        </>
    )
}
