import { Box } from "@mui/material"
import type { TaskStatus } from "../../types/backendTypes"
import { TaskStatusIcon } from "../task/TaskStatus"

export function TaskPipelineItem({
    icon,
    status
}: {
    icon: React.ReactNode
    status: TaskStatus
}) {
    return (
        <Box
            sx={{
                display: "flex",
                alignItems: "center",
                gap: 1,
                px: 1.5,
                py: 0.5,
                borderRadius: 2,
                background: "#1a1a1a",
                border: "1px solid #333",
                minWidth: 80
            }}
        >
            {icon}
            <TaskStatusIcon status={status} />
        </Box>
    )
}
