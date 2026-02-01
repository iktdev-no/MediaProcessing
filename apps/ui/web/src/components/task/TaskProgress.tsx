import { Box, LinearProgress, Typography } from "@mui/material"
import type { UiTask } from "../../types/backendTypes"

export interface TaskProgressProps {
    task: UiTask
}

export function TaskProgress({ task }: TaskProgressProps) {
    if (typeof task.progress !== "number") {
        return null
    }

    return (
        <Box mt={1}>
            <Typography variant="body2">Progress: {task.progress}%</Typography>
            <LinearProgress variant="determinate" value={task.progress} />
        </Box>
    )
}
